package locationkitapp.locationkit.locationkitapp.sugar;

import android.location.Location;

import com.orm.SugarRecord;

import java.util.Locale;

import socialradar.locationkit.internal.util.LKPlaceUtils;
import socialradar.locationkit.model.LKCoordinate;
import socialradar.locationkit.model.LKPlace;
import socialradar.locationkit.model.LKVisit;

/**
 * Created by johnfontaine on 11/30/15.
 */
public class Visit extends SugarRecord<Visit> {
    public Long detectedTime;
    public String visitId;
    public Long arrivalDate;
    public Long departureDate;
    public String category;
    public String subcategory;
    public String venueName;
    public String street;
    public String city;
    public String state;
    public String zip;
    public String detectionMethod;
    public double latitude;
    public double longitude;
    public boolean fromPlace;
    public Visit() {
    }

    public Visit(Long arrivalDate, String category, String city, Long departureDate, Long detectedTime, String detectionMethod, boolean fromPlace, double latitude, double longitude, String state, String street, String subcategory, String venueName, String visitId, String zip) {
        this.arrivalDate = arrivalDate;
        this.category = category;
        this.city = city;
        this.departureDate = departureDate;
        this.detectedTime = detectedTime;
        this.detectionMethod = detectionMethod;
        this.fromPlace = fromPlace;
        this.latitude = latitude;
        this.longitude = longitude;
        this.state = state;
        this.street = street;
        this.subcategory = subcategory;
        this.venueName = venueName;
        this.visitId = visitId;
        this.zip = zip;
    }
    public Visit(LKPlace place) {
        this.fromPlace = true;
        this.visitId = place.getIdentifier();
        this.arrivalDate = System.currentTimeMillis();
        this.venueName = LKPlaceUtils.generatePrettyPlaceString(place);
        this.departureDate = 0l;
        this.detectionMethod = "Polygon";
        applyPlaceProperties(place);
    }
    private void applyPlaceProperties(LKPlace place) {
        if (place != null && place.getVenue() != null) {
            this.category = place.getVenue().getCategory();
            this.subcategory = place.getVenue().getSubcategory();
        }
        if (place != null && place.getAddress() != null) {
            this.street = String.format(Locale.ENGLISH, "%s %s", place.getAddress().getStreetNumber(),place.getAddress().getStreetName());
            this.city = place.getAddress().getLocality();
            this.state = place.getAddress().getRegion();
            this.zip = place.getAddress().getPostalCode();
        }
        LKCoordinate l = place.getLocation();
        this.latitude = l.getLatitude();
        this.longitude =l.getLongitude();
    }
    public Visit(LKVisit visit) {
        this.visitId = LKPlaceUtils.generateVisitId(visit);
        this.fromPlace = false;
        this.arrivalDate = visit.getArrivalDate();
        this.venueName = LKPlaceUtils.generatePrettyPlaceString(visit);
        this.departureDate = visit.getDepartureDate();
        applyPlaceProperties(visit.getPlace());
        if (visit.getPlace() != null) {
            if (visit.getPlace().getUsedEntrance() == null) {
                detectionMethod = "Third Party Source";
            } else if (visit.getPlace().getUsedEntrance()) {
                detectionMethod = "Doorway Intersection";
            } else {
                detectionMethod = "Polygon";
            }
        }
    }

}
