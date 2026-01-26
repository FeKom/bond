#!/bin/bash

# Script para testar autenticação com Keycloak
# Uso: ./scripts/test-auth.sh

KEYCLOAK_URL="http://localhost:8180"
BOND_URL="http://localhost:8080"
REALM="bond"
CLIENT_ID="bond-api"
CLIENT_SECRET="bond-api-secret"

echo "=============================================="
echo "   Bond Rate Limiter - Teste de Autenticação"
echo "=============================================="
echo ""

# Função para obter token
get_token() {
    local username=$1
    local password=$2

    curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=${CLIENT_ID}" \
        -d "client_secret=${CLIENT_SECRET}" \
        -d "username=${username}" \
        -d "password=${password}" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', 'ERROR'))"
}

echo "1. Obtendo token para usuário ADMIN..."
ADMIN_TOKEN=$(get_token "admin" "admin123")
if [ "$ADMIN_TOKEN" == "ERROR" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "   ERRO: Não foi possível obter token do admin"
    echo "   Verifique se o Keycloak está rodando em ${KEYCLOAK_URL}"
    exit 1
fi
echo "   Token obtido com sucesso!"
echo ""

echo "2. Obtendo token para usuário USER..."
USER_TOKEN=$(get_token "user" "user123")
if [ "$USER_TOKEN" == "ERROR" ] || [ -z "$USER_TOKEN" ]; then
    echo "   ERRO: Não foi possível obter token do user"
    exit 1
fi
echo "   Token obtido com sucesso!"
echo ""

echo "=============================================="
echo "   Testando Endpoints"
echo "=============================================="
echo ""

echo "3. Testando /actuator/health (público)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BOND_URL}/actuator/health")
echo "   Status: $HTTP_CODE (esperado: 200)"
echo ""

echo "4. Testando /clients SEM token (deve falhar)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BOND_URL}/clients" \
    -H "Content-Type: application/json" \
    -d '{"tier": "FREE"}')
echo "   Status: $HTTP_CODE (esperado: 401)"
echo ""

echo "5. Testando /clients COM token de USER (deve falhar - sem role ADMIN)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BOND_URL}/clients" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    -d '{"tier": "FREE"}')
echo "   Status: $HTTP_CODE (esperado: 403)"
echo ""

echo "6. Testando /clients COM token de ADMIN..."
RESPONSE=$(curl -s -X POST "${BOND_URL}/clients" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -d '{"tier": "FREE"}')
echo "   Resposta: $RESPONSE"
CLIENT_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', 'ERROR'))" 2>/dev/null)
echo ""

echo "7. Testando /check COM token de USER..."
RESPONSE=$(curl -s -X POST "${BOND_URL}/check" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${USER_TOKEN}" \
    -H "X-API-Key: ${CLIENT_ID}" \
    -d '{"data": "hello world"}')
echo "   Resposta: $RESPONSE"
echo ""

echo "=============================================="
echo "   Resumo"
echo "=============================================="
echo ""
echo "Keycloak Admin Console: ${KEYCLOAK_URL}/admin"
echo "  - Usuário: admin"
echo "  - Senha: admin"
echo ""
echo "Usuários de teste:"
echo "  - admin / admin123 (role: ADMIN, USER)"
echo "  - user / user123 (role: USER)"
echo ""
echo "Para obter um token manualmente:"
echo ""
echo "curl -X POST '${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token' \\"
echo "  -H 'Content-Type: application/x-www-form-urlencoded' \\"
echo "  -d 'grant_type=password' \\"
echo "  -d 'client_id=${CLIENT_ID}' \\"
echo "  -d 'client_secret=${CLIENT_SECRET}' \\"
echo "  -d 'username=admin' \\"
echo "  -d 'password=admin123'"
echo ""
