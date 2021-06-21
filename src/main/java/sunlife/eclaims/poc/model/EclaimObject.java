package sunlife.eclaims.poc.model;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;


@Getter
@Setter
public class EclaimObject {

    @JsonProperty
    String eclaims_Eligible_c;
    @JsonProperty
    String id;
    @JsonProperty
    String _ObjectType;
}
