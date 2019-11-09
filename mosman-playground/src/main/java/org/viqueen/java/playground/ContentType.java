package org.viqueen.java.playground;

import mosman.annotations.Builder;
import mosman.annotations.Data;

@Data
@Builder
public interface ContentType {
    String name();
}
