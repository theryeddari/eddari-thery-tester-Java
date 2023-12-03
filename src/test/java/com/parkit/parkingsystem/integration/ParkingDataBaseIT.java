package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {

    }

    private static void customerJourney(int choice) {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        switch (choice) {
            case 1:
                parkingService.processIncomingVehicle();
                break;
            case 2:
                parkingService.processIncomingVehicle();
                Awaitility.await().pollDelay(1, SECONDS).until(() -> true);
                parkingService.processExitingVehicle();
                break;
            case 3:
                for (int i = 0; i < 2; i++) {
                    parkingService.processIncomingVehicle();
                    Awaitility.await().pollDelay(1, SECONDS).until(() -> true);
                    parkingService.processExitingVehicle();
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + choice);
        }
    }

    @Test
    void testParkingACar() {
        customerJourney(1);
        Ticket checkTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(checkTicket);
        assertFalse(checkTicket.getParkingSpot().isAvailable());
    }

    @Test
    void testParkingLotExit() {
        customerJourney(2);

        Ticket checkTicket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(checkTicket.getOutTime());
        long timeOut = checkTicket.getOutTime().getTime();
        long nowTime = new Date().getTime();
        assertEquals(nowTime, timeOut, (1000)); // 1 secondes de délai max connexion a la base etc.
        double gapTimeDecimal = (double) (checkTicket.getOutTime().getTime() - checkTicket.getInTime().getTime()) / (60 * 60 * 1000);
        double priceExpected = gapTimeDecimal * Fare.CAR_RATE_PER_HOUR;
        assertEquals(priceExpected, checkTicket.getPrice(), 0.1);
    }

    @Test
    void testParkingLotExitRecurringUser() {
        customerJourney(3);

        Ticket checkTicket = ticketDAO.getTicket("ABCDEF");

        assertTrue(ticketDAO.getNbTicket("ABCDEF"));
        assertNotNull(checkTicket.getOutTime());
        double gapTimeDecimal = (double) (checkTicket.getOutTime().getTime() - checkTicket.getInTime().getTime()) / (60 * 60 * 1000);
        double priceExpected = gapTimeDecimal * Fare.CAR_RATE_PER_HOUR * 0.95;
        assertEquals(priceExpected, checkTicket.getPrice(), 0.1);
    }
}
