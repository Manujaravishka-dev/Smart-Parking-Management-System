package com.spms.vehicle.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spms.vehicle.dto.request.RegisterVehicleRequest;
import com.spms.vehicle.dto.request.UpdateVehicleRequest;
import com.spms.vehicle.dto.response.VehicleResponse;
import com.spms.vehicle.service.VehicleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> register(@Valid @RequestBody RegisterVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.register(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicle(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByUser(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/entry")
    public ResponseEntity<VehicleResponse> vehicleEntry(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.vehicleEntry(id));
    }

    @PostMapping("/{id}/exit")
    public ResponseEntity<VehicleResponse> vehicleExit(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.vehicleExit(id));
    }
}
