package com.o3.server;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class ObservationRecord {
    private String targetBodyName;
    private String centerBodyName;
    private String epoch;

    private JSONObject orbitalElements;
    private JSONObject stateVector;

    public ObservationRecord(JSONObject json) {
        this.targetBodyName = json.getString("target_body_name");
        this.centerBodyName = json.getString("center_body_name");
        this.epoch = json.getString("epoch");

        if (json.has("orbital_elements")) {
            JSONObject orb_elements = json.getJSONObject("orbital_elements");

            orb_elements.getDouble("semi_major_axis_au");
            orb_elements.getDouble("eccentricity");
            orb_elements.getDouble("inclination_deg");
            orb_elements.getDouble("longitude_ascending_node_deg");
            orb_elements.getDouble("argument_of_periapsis_deg");
            orb_elements.getDouble("mean_anomaly_deg");

            this.orbitalElements = orb_elements;
        }

        if (json.has("state_vector")) {
            JSONObject s_vector = json.getJSONObject("state_vector");

            validateNumericArray(s_vector.getJSONArray("position_au"), 3);
            validateNumericArray(s_vector.getJSONArray("velocity_au_per_day"), 3);

            this.stateVector = s_vector;
        }

    }

    private void validateNumericArray(JSONArray array, int expectedSize) throws JSONException {
            if (array.length() != expectedSize) {
                throw new JSONException("Wrong number of elements in array");
            }
            for (int i = 0; i < array.length(); i++) {
                array.getDouble(i);
            }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("target_body_name", targetBodyName);
        json.put("center_body_name", centerBodyName);
        json.put("epoch", epoch);
    
        if (orbitalElements != null) {
            json.put("orbital_elements", orbitalElements);
        }

        if (stateVector != null) {
            json.put("state_vector", stateVector);
        }

        return json;
    }
}
