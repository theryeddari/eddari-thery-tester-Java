package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ParkingSpotDAO {
    private static final Logger logger = LogManager.getLogger("ParkingSpotDAO");

    private DataBaseConfig dataBaseConfig = new DataBaseConfig();

    public void setDataBaseConfig(DataBaseConfig dataBaseConfig) {
        this.dataBaseConfig = dataBaseConfig;
    }

    public int getNextAvailableSlot(ParkingType parkingType){
        try (Connection con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)){
            ps.setString(1, parkingType.toString());
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
            return 0;
        }
    }

    public void updateParking(ParkingSpot parkingSpot) {
        //update the availability fo that parking slot
        try (Connection con = dataBaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(DBConstants.UPDATE_PARKING_SPOT)) {
            ps.setBoolean(1, parkingSpot.isAvailable());
            ps.setInt(2, parkingSpot.getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            logger.error("Error updating parking info", ex);
        }
    }
}
