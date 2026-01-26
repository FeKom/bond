-- Adiciona coluna bucket para armazenar o estado do TokenBucket como JSON
ALTER TABLE rate_limiters ADD COLUMN IF NOT EXISTS bucket JSONB;
