package eu.europeana.metis.schema.convert.model;

import java.util.Set;

public record ResourceInfo(Set<UrlType> urlTypes, boolean configuredForOembed) {}
