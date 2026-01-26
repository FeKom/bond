package github.fekom.bond.infrastructure.web;

import github.fekom.bond.api.ClientService;
import github.fekom.bond.api.dto.in.Client.CreateClientRequest;
import github.fekom.bond.api.dto.out.Client.CreateClientResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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

	@GetMapping("/tier/{id}")
	public ResponseEntity<?> getClientTier(@PathVariable String id) {
		try {
			var tierOpt = service.getTierById(id);
			if (tierOpt.isPresent()) {
				return ResponseEntity.ok(tierOpt.get());
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found");
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				"Error retrieving client tier: " + e.getMessage()
			);
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getById(@PathVariable String id) {
		try {
			var clientOpt = service.getById(id);
			if (clientOpt.isPresent()) {
				return ResponseEntity.ok(clientOpt.get());
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found");
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving client: " + e.getMessage());
		}
	}
}
