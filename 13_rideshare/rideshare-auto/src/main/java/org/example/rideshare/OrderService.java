package org.example.rideshare;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;


@Service
public class OrderService {

    public static final Duration OP_DURATION = Duration.of(200, ChronoUnit.MILLIS);

    // Statische Liste zur Speicherung der allokierten Arrays
    private static final List<byte[]> MEMORY_BLOAT = new ArrayList<>();

    private final MeterRegistry meterRegistry;

    private final Timer checkDriverAvailabilityTimer;
    private final Timer mutexLockTimer;

    private final Counter findNearestVehicleCalls;
    private final Counter memoryBloatAllocations;

    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.checkDriverAvailabilityTimer = Timer.builder("rideshare.order.check_driver_availability")
                .description("Time spent checking driver availability")
                .publishPercentileHistogram()
                .register(meterRegistry);

        this.mutexLockTimer = Timer.builder("rideshare.order.mutex_lock")
                .description("Artificial lock contention / delay")
                .publishPercentileHistogram()
                .register(meterRegistry);

        this.findNearestVehicleCalls = Counter.builder("rideshare.order.find_nearest_vehicle.calls")
                .description("Number of findNearestVehicle calls")
                .register(meterRegistry);

        this.memoryBloatAllocations = Counter.builder("rideshare.order.memory_bloat.allocations")
                .description("Number of 2MB allocations done for the memory demo")
                .register(meterRegistry);
    }

    @PostConstruct
    void registerGauges() {
        // Keep metrics low-cardinality: do NOT tag by vehicle id or other unbounded values.
        Gauge.builder("rideshare.order.memory_bloat.chunks", MEMORY_BLOAT, List::size)
                .description("Number of allocated 2MB chunks retained in MEMORY_BLOAT")
                .register(meterRegistry);

        Gauge.builder("rideshare.order.memory_bloat.bytes", MEMORY_BLOAT,
                        list -> (double) list.size() * 2d * 1024d * 1024d)
                .description("Estimated bytes retained in MEMORY_BLOAT")
                .register(meterRegistry);
    }

    public synchronized void findNearestVehicle(int searchRadius, String vehicle) {
        findNearestVehicleCalls.increment();

        // Keep tags bounded. Example: bucket searchRadius instead of tagging raw values.
        final String radiusBucket;
        if (searchRadius <= 1) {
            radiusBucket = "1";
        } else if (searchRadius <= 3) {
            radiusBucket = "2-3";
        } else if (searchRadius <= 10) {
            radiusBucket = "4-10";
        } else {
            radiusBucket = "11+";
        }

        final Timer timer = meterRegistry.timer(
                "rideshare.order.find_nearest_vehicle",
                "vehicle", vehicle,
                "radius", radiusBucket
        );
        final Timer.Sample sample = Timer.start(meterRegistry);
        try {
            AtomicLong i = new AtomicLong();
            Instant end = Instant.now().plus(OP_DURATION.multipliedBy(searchRadius));
            while (Instant.now().compareTo(end) <= 0) {
                i.incrementAndGet();
            }

            if (vehicle.equals("car")) {
                checkDriverAvailability(searchRadius);
            }
            if (vehicle.equals("scooter")) {
                final String envMemory = System.getenv("ENABLE_MEMORY_TEST");
                if (envMemory != null && envMemory.equalsIgnoreCase("true")) {
                    simulateMemoryUsage(searchRadius);
                }
            }
        } finally {
            sample.stop(timer);
        }
    }

    private void checkDriverAvailability(int searchRadius) {
        // final String envMemory = System.getenv("ENABLE_MEMORY_TEST");
        // if (envMemory != null && envMemory.equalsIgnoreCase("true")) {
        //     simulateMemoryUsage(searchRadius);
        // }
        
        checkDriverAvailabilityTimer.record(() -> {
            AtomicLong i = new AtomicLong();
            Instant end = Instant.now().plus(OP_DURATION.multipliedBy(searchRadius));
            while (Instant.now().compareTo(end) <= 0) {
                i.incrementAndGet();
            }
        });

        // Every other minute this will artificially create make requests in eu-north region slow
        // this is just for demonstration purposes to show how performance impacts show up in the
        // flamegraph
        // boolean force_mutex_lock = Instant.now().atZone(ZoneOffset.UTC).getMinute() % 2 == 0;
        // if (System.getenv("REGION").equals("eu-north") && force_mutex_lock) {
        final String envLock = System.getenv("ENABLE_LOCK_TEST");
        if (envLock != null && envLock.equalsIgnoreCase("true")) {
            mutexLock(searchRadius);
        }
    }

    private void mutexLock(int searchRadius) {
        mutexLockTimer.record(() -> {
            AtomicLong i = new AtomicLong();
            Instant end = Instant.now().plus(OP_DURATION.multipliedBy(30L * searchRadius));
            while (Instant.now().compareTo(end) <= 0) {
                i.incrementAndGet();
            }
        });
    }

    private void simulateMemoryUsage(int searchRadius) {
        // pro iteration wird 2 MB allokiert und in der Liste gehalten
        for (int j = 0; j < searchRadius; j++) {
            MEMORY_BLOAT.add(new byte[2 * (1024 * 1024)]);
            memoryBloatAllocations.increment();
        }
    }

}
