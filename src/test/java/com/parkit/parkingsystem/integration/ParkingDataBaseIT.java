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
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket checkTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(checkTicket);
        assertFalse(checkTicket.getParkingSpot().isAvailable());
    }

    @Test
    void testParkingLotExit(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Awaitility.await().pollDelay(1, SECONDS).until(() -> true);
        parkingService.processExitingVehicle();

        Ticket checkTicket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(checkTicket.getOutTime());
        long timeOut = checkTicket.getOutTime().getTime();
        long nowTime = new Date().getTime();
        assertEquals(nowTime, timeOut,(1000)); // 1 secondes de délai max connexion a la base etc.
        double gapTimeDecimal = (double) (checkTicket.getOutTime().getTime() - checkTicket.getInTime().getTime()) / (60 * 60 * 1000);
        double priceExpected = gapTimeDecimal * Fare.CAR_RATE_PER_HOUR;
        assertEquals(priceExpected,checkTicket.getPrice(),0.1);
    }
    @Test
    void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        await().pollDelay(5, SECONDS).until(() -> true);
        parkingService.processExitingVehicle();
        parkingService.processIncomingVehicle();
        await().pollDelay(5, SECONDS).until(() -> true);
        parkingService.processExitingVehicle();

        Ticket checkTicket = ticketDAO.getTicket("ABCDEF");

        assertTrue(ticketDAO.getNbTicket("ABCDEF"));
        assertNotNull(checkTicket.getOutTime());
        double gapTimeDecimal = (double) (checkTicket.getOutTime().getTime() - checkTicket.getInTime().getTime()) / (60 * 60 * 1000);
        double priceExpected = gapTimeDecimal * Fare.CAR_RATE_PER_HOUR * 0.95;
        assertEquals(priceExpected,checkTicket.getPrice(),0.1);
        //TODO : refactor ParkingDataBaseIT
    }
}
