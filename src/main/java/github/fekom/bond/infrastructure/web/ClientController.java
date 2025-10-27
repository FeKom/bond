package github.fekom.bond.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import github.fekom.bond.api.ClientService;
import github.fekom.bond.api.dto.in.Client.CreateClientRequest;
import github.fekom.bond.api.dto.out.Client.CreateClientResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/clients")
public class ClientController {
	private final ClientService service;

	public ClientController(ClientService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<?> createClient(@Valid @RequestBody CreateClientRequest request) {
		try {
			var client = service.create(request.tier());
			var response = CreateClientResponse.fromDomain(client, "Client created successfully");
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating client: " + e.getMessage());
		}
	}
}
