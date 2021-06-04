package sunlife.eclaims.poc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Header {
    @JsonProperty
    public String key;
    @JsonProperty
    public String stringValue;
}
