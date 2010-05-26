package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.wms.GenericServiceAndBaseLayerSupport;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.ala.spatial.search.TaxaCommonSearchResult;
import org.ala.spatial.search.TaxaCommonSearchSummary;
import org.ala.spatial.util.Layer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.zkoss.zhtml.Iframe;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Radio;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class MaxentWCController extends UtilityComposer {

    private static final long serialVersionUID = 165701023268014945L;
    private static final String GEOSERVER_URL = "geoserver_url";
    private static final String GEOSERVER_USERNAME = "geoserver_username";
    private static final String GEOSERVER_PASSWORD = "geoserver_password";
    private static final String SAT_URL = "sat_url";
    private Radio rdoCommonSearch;
    private SpeciesAutoComplete sac;
    private Button btnMapSpecies;
    private Label status;
    private Label infourl;
    private Button btnInfo;
    private Textbox tbenvfilter;
    private Button btenvfilterclear;
    private Listbox lbenvlayers;
    private Button startmaxent;
    private List<Layer> _layers;
    private Checkbox chkJackknife;
    private Checkbox chkRCurves;
    private Textbox txtTestPercentage;
    private Textbox trun;
    //private SpatialSettings ssets;
    private Tabbox outputtab;
    private Iframe mapframe;
    private Iframe infoframe;
    private Window maxentWindow;
    private Window maxentInfoWindow;
    private GenericServiceAndBaseLayerSupport genericServiceAndBaseLayerSupport;
    private MapComposer mc;
    private String geoServer = "http://ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com";  // http://localhost:8080
    private String satServer = geoServer;
    private SettingsSupplementary settingsSupplementary = null;

    @Override
    public void doAfterCompose(Component component) throws Exception {
        super.doAfterCompose(component);
    }

    /**
     * When the page is loaded, setup the various settings that are needed
     * throughtout the page action
     *
     * @param comp The page component itself
     * @throws Exception
     */
    @Override
    public void afterCompose() {
        super.afterCompose();

        try {
            //Messagebox.show("Hello world afterCompose!");
            mc = getThisMapComposer();
            if (settingsSupplementary != null) {
                geoServer = settingsSupplementary.getValue(GEOSERVER_URL);
                satServer = settingsSupplementary.getValue(SAT_URL);
            }

            setupEnvironmentalLayers();
        } catch (Exception e) {
            System.out.println("opps in after compose");
        }

    }

    /**
     * Iterate thru' the layer list setup in the @doAfterCompose method
     * and setup the listbox
     */
    private void setupEnvironmentalLayers() {
        try {

            String envurl = satServer + "/alaspatial/ws/spatial/settings/layers/environmental/string";

            //Messagebox.show("Loading env data from: " + envurl);


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(envurl);
            get.addRequestHeader("Content-type", "text/plain");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            //Messagebox.show("done loading data: " + slist);

            String[] aslist = slist.split("\n");

            if (aslist.length > 0) {

                lbenvlayers.setItemRenderer(new ListitemRenderer() {

                    public void render(Listitem li, Object data) {
                        li.setWidth(null);
                        new Listcell((String) data).setParent(li);
                    }
                });

                lbenvlayers.setModel(new SimpleListModel(aslist));
            }


        } catch (Exception e) {
            System.out.println("error setting up env list");
            e.printStackTrace(System.out);
        }
    }

    public void onCheck$rdoCommonSearch() {
        sac.setSearchCommon(true);
        sac.getItems().clear();
    }

    public void onCheck$rdoScientificSearch() {
        sac.setSearchCommon(false);
        sac.getItems().clear();
    }

    public void onChange$sac(Event event) {
        status.setValue("Selected species: " + sac.getValue());
        if (rdoCommonSearch.isChecked()) {
            status.setValue("Selected species: " + getScientificName() + " (" + sac.getValue() + ")");
        }
    }

    public void onClick$btnMapSpecies(Event event) {
        try {
            //status.setValue("clicked new value selected: " + sac.getText() + " - " + sac.getValue());
            //System.out.println("Looking up taxon names for " + sac.getValue());
            //Messagebox.show("Hello world!, i got clicked");

            loadSpeciesOnMap();

        } catch (Exception ex) {
            System.out.println("Got an error clicking button!!");
            ex.printStackTrace(System.out);
        }
    }

    /**
     * On changing the text box value, iterate thru' the listbox
     * and display only the matched options.
     *
     * @param event The event attached to the component
     */
    public void onChanging$tbenvfilter(InputEvent event) {
        String filter = event.getValue().toLowerCase();
        System.out.println("checking for: " + filter);
        System.out.print("Number of list items to iterate thru: ");
        System.out.println(lbenvlayers.getItems().size());
        for (Listitem li : (List<Listitem>) lbenvlayers.getItems()) {
            if (li.getLabel().toLowerCase().contains(filter)) {
                if (!li.isVisible()) {
                    li.setVisible(true);
                }
            } else {
                if (li.isVisible()) {
                    li.setVisible(false);
                }
            }
        }
    }

    /**
     * Clear the filter text box
     *
     * @param event The event attached to the component
     */
    public void onClick$btenvfilterclear(Event event) {
        try {
            tbenvfilter.setValue("");
            for (Listitem li : (List<Listitem>) lbenvlayers.getItems()) {
                li.setVisible(true);
            }
            //Messagebox.show("Cleared env list");
        } catch (Exception ex) {
            Logger.getLogger(MaxentWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Clicked on clear env list button");
        }
    }

    public void onClick$startmaxent(Event event) {
        try {
            String taxon = sac.getValue();
            // check if its a common name, if so, grab the scientific name
            if (rdoCommonSearch.isChecked()) {
                taxon = getScientificName();
            }

            String msg = "";
            String[] envsel = null;
            StringBuffer sbenvsel = new StringBuffer();

            status.setValue("Status: Running Maxent, please wait... ");
            btnInfo.setVisible(false);

            if (lbenvlayers.getSelectedCount() > 0) {
                envsel = new String[lbenvlayers.getSelectedCount()];
                msg = "Selected " + lbenvlayers.getSelectedCount() + " items \n ";
                Iterator it = lbenvlayers.getSelectedItems().iterator();
                int i = 0;
                while (it.hasNext()) {
                    Listitem li = (Listitem) it.next();

                    sbenvsel.append(li.getLabel());
                    if (it.hasNext()) {
                        sbenvsel.append(":");
                    }

                }
            }

            //process(envsel);

            System.out.println("Selected species: " + taxon);
            System.out.println("Selected env vars");
            System.out.println(sbenvsel.toString());
            System.out.println("Selected options: ");
            System.out.println("Jackknife: " + chkJackknife.isChecked());
            System.out.println("Response curves: " + chkRCurves.isChecked());
            System.out.println("Test per: " + txtTestPercentage.getValue());


            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append(satServer + "/alaspatial/ws/maxent/process?");
            sbProcessUrl.append("taxonid=" + URLEncoder.encode(taxon, "UTF-8"));
            sbProcessUrl.append("&envlist=" + URLEncoder.encode(sbenvsel.toString(), "UTF-8"));
            if (chkJackknife.isChecked()) {
                sbProcessUrl.append("&chkJackknife=on");
            }
            if (chkRCurves.isChecked()) {
                sbProcessUrl.append("&chkResponseCurves=on");
            }
            sbProcessUrl.append("&txtTestPercentage=" + txtTestPercentage.getValue());


            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(sbProcessUrl.toString());
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();

            System.out.println("Got response from MaxentWSController: \n" + slist);

            String[] maxentresponse = slist.split(";");
            String[] status = maxentresponse[0].split(":");
            String[] pid = maxentresponse[1].split(":");
            String[] info = maxentresponse[2].split(":");

            String mapurl = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:species_" + pid[1] + "&styles=alastyles&srs=EPSG:4326&TRANSPARENT=true&FORMAT=image%2Fpng";

            String legendurl = geoServer
                    + "/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=10&HEIGHT=20"
                    + "&LAYER=ALA:species_" + pid[1]
                    + "&STYLE=alastyles";

            System.out.println(legendurl);

            //get the current MapComposer instance
            //MapComposer mc = getThisMapComposer();

            mc.addWMSLayer("Maxent model for " + taxon, mapurl, (float) 0.5, "", legendurl);

            this.status.setValue("Status: " + status[1]);
            if (status[1].equalsIgnoreCase("success")) {
                if (info.length == 2) {
                    infourl.setValue("Show process information");
                    showInfoWindow(info[1]);
                }
            }

            infourl.setValue(info[1]);
            btnInfo.setVisible(true);

            //Messagebox.show(msg, "Maxent", Messagebox.OK, Messagebox.INFORMATION);
        } catch (Exception e) {
            System.out.println("Maxent error: ");
            e.printStackTrace(System.out);
        }


    }

    public void onClick$btnInfo(Event event) {
        try {
            showInfoWindow(infourl.getValue());
        } catch (Exception e) {
            System.out.println("opps");
        }
    }

    private void showInfoWindow(String url) {
        Map args = new Hashtable();
        args.put("url", satServer + "/alaspatial" + url);
        if (maxentInfoWindow == null) {
            maxentInfoWindow = (Window) Executions.createComponents(
                    "/WEB-INF/zul/AnalysisMaxentInfo.zul", this, args);
        } else {
            maxentInfoWindow.detach();
            maxentInfoWindow = (Window) Executions.createComponents(
                    "/WEB-INF/zul/AnalysisMaxentInfo.zul", this, args);
        }

        maxentInfoWindow.setId(java.util.UUID.randomUUID().toString());
        maxentInfoWindow.setMaximizable(true);
        maxentInfoWindow.setPosition("center");
        maxentInfoWindow.doOverlapped();
    }

    /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        //Page page = maxentWindow.getPage();
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    private void loadSpeciesOnMap() {
        /*
        String taxon = sac.getValue();
        String uri = null;
        String filter = null;

        // capitalise the taxon name
        System.out.print("Changing taxon name from '" + taxon);
        taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1);
        System.out.println("' to '" + taxon + "' ");


        //uri = "http://ec2-184-73-34-104.compute-1.amazonaws.com/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:occurrencesv1&styles=&srs=EPSG:4326&format=image/png";
        uri = geoServer + "/geoserver/wms?service=WMS&version=1.1.0&request=GetMap&layers=ALA:occurrencesv1&styles=&srs=EPSG:4326&format=image/png";

        //get the current MapComposer instance
        MapComposer mc = getThisMapComposer();

        //contruct the filter
        //filter = "<Filter><PropertyIsEqualTo><PropertyName>url</PropertyName><Literal><![CDATA["+mapWMS+entity+"&type=1&unit=1]]></Literal></PropertyIsEqualTo></Filter>";
        //lets try it in cql
        //filter = "species eq '" + taxon + "'";
        String desc = sac.getSelectedItem().getDescription(); 
        String tlevel = desc.split("-")[0].trim();
        filter =  tlevel + " eq '" + taxon + "'";
        System.out.println("Setting filter: " + filter );

        logger.debug(filter);
        //mc.addWMSLayer(label, uri, 1, filter);
        mc.addKnownWMSLayer(taxon, uri, 1, filter);
         *
         */

        String taxon = sac.getValue();
        // check if its a common name, if so, grab the scientific name
        if (rdoCommonSearch.isChecked()) {
            taxon = getScientificName();
        }
        taxon = taxon.substring(0, 1).toUpperCase() + taxon.substring(1);
        String uri = null;
        String filter = null;
        String entity = null;
        MapLayer mapLayer = null;

        //TODO these paramaters need to read from the config
        String layerName = "ALA:occurrencesv1";
        String sld = "species_point";
        uri = geoServer + "/geoserver/wms?service=WMS";
        String format = "image/png";

        //get the current MapComposer instance
        //MapComposer mc = getThisMapComposer();

        //contruct the filter in cql
        filter = "species eq '" + taxon + "'";
        mapLayer = genericServiceAndBaseLayerSupport.createMapLayer("Species occurrence for " + taxon, taxon, "1.1.1", uri, layerName, format, sld, filter);

        //create a random colour

        Random rand = new java.util.Random();
        int r = rand.nextInt(99);
        int g = rand.nextInt(99);
        int b = rand.nextInt(99);
        String hexColour = String.valueOf(r) + String.valueOf(g) + String.valueOf(b);
        mapLayer.setEnvParams("color:" + hexColour + ";name:circle;size:6");
        mc.addUserDefinedLayerToMenu(mapLayer, true);



    }

    private String getScientificName() {
        String taxon = "";
        try {

            String nuri = "http://data.ala.org.au/search/commonNames/" + URLEncoder.encode(sac.getValue(), "UTF-8") + "/json";
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(nuri);
            get.addRequestHeader("Content-type", "application/json");

            int result = client.executeMethod(get);
            String snlist = get.getResponseBodyAsString();

            TaxaCommonSearchSummary tss = new TaxaCommonSearchSummary();
            JsonConfig jsonConfig = new JsonConfig();
            jsonConfig.setRootClass(TaxaCommonSearchSummary.class);
            jsonConfig.setJavaPropertyFilter(new PropertyFilter() {

                @Override
                public boolean apply(Object source, String name, Object value) {
                    if ("result".equals(name)) {
                        return true;
                    }
                    return false;
                }
            });

            JSONObject jo = JSONObject.fromObject(snlist);

            tss = (TaxaCommonSearchSummary) JSONSerializer.toJava(jo, jsonConfig);

            if (tss.getRecordsReturned() > 1) {

                JSONArray joResult = jo.getJSONArray("result");

                JsonConfig jsonConfigResult = new JsonConfig();
                jsonConfigResult.setRootClass(TaxaCommonSearchResult.class);

                for (int i = 0; i < joResult.size(); i++) {
                    TaxaCommonSearchResult tr = (TaxaCommonSearchResult) JSONSerializer.toJava(joResult.getJSONObject(i), jsonConfigResult);
                    tss.addResult(tr);
                }
            }

            //taxon = tss.getResultList().get(0).getScientificName() + " (" + tss.getResultList().get(0).getCommonName() + ")";
            taxon = tss.getResultList().get(0).getScientificName();
            //status.setValue("Got: " + tss.getResultList().get(0).getScientificName() + " (" + tss.getResultList().get(0).getCommonName() + ")");

        } catch (Exception e) {
            System.out.println("Oopps, error getting scientific name from common name");
            e.printStackTrace(System.out);
        }

        return taxon;
    }
}