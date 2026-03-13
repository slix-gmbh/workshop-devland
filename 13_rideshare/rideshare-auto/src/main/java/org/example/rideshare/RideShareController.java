package org.example.rideshare;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.example.rideshare.bike.BikeService;
import org.example.rideshare.car.CarService;
import org.example.rideshare.scooter.ScooterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RideShareController {

    private final CarService carService;
    private final ScooterService scooterService;
    private final BikeService bikeService;
    private final MeterRegistry meterRegistry;

    public RideShareController(CarService carService,
                               ScooterService scooterService,
                               BikeService bikeService,
                               MeterRegistry meterRegistry) {
        this.carService = carService;
        this.scooterService = scooterService;
        this.bikeService = bikeService;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/bike")
    public String orderBike() {
        final int searchRadius = 1;
        final Timer timer = meterRegistry.timer("rideshare.http.order", "vehicle", "bike", "radius", "1");
        final Timer.Sample sample = Timer.start(meterRegistry);
        try {
            bikeService.orderBike(searchRadius);
            return "<h1>Bike ordered</h1>";
        } finally {
            sample.stop(timer);
        }
    }

    @GetMapping("/scooter")
    public String orderScooter() {
        final int searchRadius = 2;
        final Timer timer = meterRegistry.timer("rideshare.http.order", "vehicle", "scooter", "radius", "2-3");
        final Timer.Sample sample = Timer.start(meterRegistry);
        try {
            scooterService.orderScooter(searchRadius);
            return "<h1>Scooter ordered</h1>";
        } finally {
            sample.stop(timer);
        }
    }

    @GetMapping("/car")
    public String orderCar() {
        final int searchRadius = 3;
        final Timer timer = meterRegistry.timer("rideshare.http.order", "vehicle", "car", "radius", "2-3");
        final Timer.Sample sample = Timer.start(meterRegistry);
        try {
            carService.orderCar(searchRadius);
            return "<h1>Car ordered</h1>";
        } finally {
            sample.stop(timer);
        }
    }

    @GetMapping("/")
    public String env() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> it : System.getenv().entrySet()) {
            sb.append(it.getKey()).append(" = ").append(it.getValue()).append("<br>\n");
        }
        return sb.toString();
    }
}
