/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.spatial.data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Adam
 */
public interface Query {
    /**
     * Get records for this query for the provided fields.
     *
     * @param fields QueryFields to return in the sample.
     * @return records as String in CSV format.
     */
    String sample(ArrayList<QueryField> fields);

    /**
     * Get species list for this query.
     *
     * @return species list as String containing CSV.
     */
    String speciesList();

    /**
     * Get number of occurrences in this query.
     *
     * @return number of occurrences as int or -1 on error.
     */
    int getOccurrenceCount();

    /**
     * Get number of species in this query.
     *
     * @return number of species as int or -1 on error.
     */
    int getSpeciesCount();
        
    /**
     * Get parsed coordinates and optional field data for this query.
     *
     * @param fields QueryFields to return in the sample as ArrayList<QueryField>.
     * If a QueryField isStored() it will be populated with the field data.
     * @return coordinates as double [] like [lng, lat, lng, lat, ...].
     */
    double[] getPoints(ArrayList<QueryField> fields);

    /**
     * Get unique term.
     *
     * @return
     */
    public String getQ();

    /**
     * Query data name.
     *
     * @return
     */
    public String getName();

    /**
     * Query data rank.
     *
     * @return
     */
    public String getRank();

    /**
     * Create new Query after adding wkt.
     *
     * @param wkt
     * @return
     */
    Query newWkt(String wkt);

    /**
     * Create new Query after adding facet.
     * 
     * @param facet
     * @return
     */
    Query newFacet(Facet facet);

    /**
     * Create new Query after adding facets.
     *
     * @param facet
     * @return
     */
    Query newFacets(List<Facet> facet);

    /**
     * Get WMS server path.
     *
     * @return
     */
    String getWMSpath();

    /**
     * Get list of available facets.
     *
     * @return
     */
    ArrayList<QueryField> getFacetFieldList();

    /**
     * Get the name of the LSID field.
     * @return
     */
    public String getSpeciesIdFieldName();

    /**
     * Get the name of the unique id field.
     * @return
     */
    public String getRecordIdFieldName();

    /**
     * Get legend data for a facet.
     *
     * @param colourmode name of field for facet.
     * @return
     */
    public LegendObject getLegend(String colourmode);

    public String getUrl();

    public List<Double> getBBox();
}