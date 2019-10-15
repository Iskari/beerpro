package ch.beerpro.domain.models;

import androidx.annotation.Nullable;

import java.io.Serializable;

public class BeerPlace implements Serializable {
    private double latitude;
    private double longitude;
    private String address;
    private String name;
    private String id;

    public BeerPlace(String id, String name, String address, double latitude, double longitude){
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public BeerPlace(){

    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getAddress() {
        return address;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof BeerPlace){
            BeerPlace other = (BeerPlace) obj;
            if(this.id != null){
                return this.id.equals(other.getId());
            }else {
                return this.getLatitude() == other.getLatitude() && this.getLongitude() == other.getLongitude();
            }
        }
        else{
            return false;
        }
    }
}
