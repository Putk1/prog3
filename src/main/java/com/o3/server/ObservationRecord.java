package com.o3.server;

import org.json.JSONObject;

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
            this.orbitalElements = json.getJSONObject("orbital_elements");
        }

        if (json.has("state_vector")) {
                this.stateVector = json.getJSONObject("state_vector");
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
