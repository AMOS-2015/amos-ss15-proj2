package org.croudtrip.utils;

/** Used to exchange data between fragments
 * Created by nazeehammari on 12/05/15.
 */

public class DataHolder {
    private static DataHolder dataObject = null;

    private DataHolder() {
    }

    public static DataHolder getInstance() {
        if (dataObject == null)
            dataObject = new DataHolder();
        return dataObject;
    }
    private int vehicle_id;

    public int getVehicle_id() {
        return vehicle_id;
    }

    public void setVehicle_id(int vehicle_id) {
        this.vehicle_id = vehicle_id;
    }
}