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

    private String recordTimeReceived;
    private String recordOwner;
    private long id;
    private String recordPayload;

    private JSONArray observatory;

    private String updateReason;
    private String editedTime;

    public ObservationRecord(JSONObject json, String nickname, long id, String timestamp) {
        this.targetBodyName = json.getString("target_body_name");
        this.centerBodyName = json.getString("center_body_name");
        this.epoch = json.getString("epoch");
        
        this.recordOwner = nickname;
        this.id = id;
        this.recordTimeReceived = timestamp;

        if (json.has("metadata")) {
            JSONObject clientMetadata = json.getJSONObject("metadata");
            
            if (!clientMetadata.has("record_payload")) {
                throw new JSONException("Missing field: record_payload");
            }
            this.recordPayload = clientMetadata.getString("record_payload");
            
            if (clientMetadata.has("observatory")) {
                this.observatory = clientMetadata.getJSONArray("observatory");

                for (int i = 0; i < this.observatory.length(); i++) {
                    JSONObject obs = this.observatory.getJSONObject(i);

                    obs.getDouble("latitude");
                    obs.getDouble("longitude");

                    // Genuinely the instructions were so confusing I'm not sure whether I need to allow users to post their own data but let's try this
                    if (obs.has("weather")) {
                        obs.getJSONObject("weather");
                    }
                }
            }

            if (clientMetadata.has("update_reason")) {
                this.updateReason = clientMetadata.getString("update_reason");
            }
            if (clientMetadata.has("edited")) {
                this.editedTime = clientMetadata.getString("edited");
            }

        } else {
            throw new JSONException("Missing record_payload");
        }

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

        JSONObject metadata = new JSONObject();
        metadata.put("record_time_received", recordTimeReceived);
        metadata.put("record_owner", recordOwner);
        metadata.put("id", id);

        if (recordPayload != null) {
            metadata.put("record_payload", recordPayload);
        }

        if (observatory != null) {
            metadata.put("observatory", observatory);
        }

        if (updateReason != null) {
            metadata.put("update_reason", updateReason);
        }

        if (editedTime != null) {
            metadata.put("edited", editedTime);
        }

        json.put("metadata", metadata);

        return json;
    }

    public String getRecordOwner() {
        return this.recordOwner;
    }
}
