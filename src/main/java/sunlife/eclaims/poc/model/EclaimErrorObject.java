package sunlife.eclaims.poc.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

//import java.time.Instant;

import java.util.LinkedList;

@Getter
@Setter
public class EclaimErrorObject {

    @JsonProperty
    public String topic;
    @JsonProperty
    public Integer  partition;
    @JsonProperty
    public Integer  offset;
    @JsonProperty
    public Integer timestamp;
    @JsonProperty
    public String  timestampType;
    @JsonProperty
    public LinkedList<Header> headers;
}
