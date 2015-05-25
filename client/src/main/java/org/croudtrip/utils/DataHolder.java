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
    private boolean isLast = false;
    private int vehicle_id;
    private String vehicle_type;

    public int getVehicle_id() {
        return vehicle_id;
    }

    public void setVehicle_id(int vehicle_id) {
        this.vehicle_id = vehicle_id;
    }

    public String getVehicle_type() {return vehicle_type;}

    public void setVehicle_type(String vehicle_type) {this.vehicle_type = vehicle_type;}



    //isLast is a flag that should be set to true when there is only one car left, in order to default to it automatically
    public boolean getIsLast() {
        return isLast;
    }

    public void setIsLast(boolean isLast) {
        this.isLast = isLast;
    }

}