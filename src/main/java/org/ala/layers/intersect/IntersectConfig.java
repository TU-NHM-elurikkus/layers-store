/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ala.layers.intersect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.layers.dao.FieldDAO;
import org.ala.layers.dao.LayerDAO;
import org.ala.layers.dto.Field;
import org.ala.layers.dto.IntersectionFile;
import org.ala.layers.dto.Layer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 *
 * @author Adam
 */
public class IntersectConfig {

    /** log4j logger */
    private static final Logger logger = Logger.getLogger(IntersectConfig.class);
    static final String ALASPATIAL_OUTPUT_PATH = "ALASPATIAL_OUTPUT_PATH";
    static final String LAYER_FILES_PATH = "LAYER_FILES_PATH";
    static final String LAYER_INDEX_URL = "LAYER_INDEX_URL";
    static final String BATCH_THREAD_COUNT = "BATCH_THREAD_COUNT";
    static final String CONFIG_RELOAD_WAIT = "CONFIG_RELOAD_WAIT";
    static final String PRELOADED_SHAPE_FILES = "PRELOADED_SHAPE_FILES";
    static final String GRID_BUFFER_SIZE = "GRID_BUFFER_SIZE";
    static final String GRID_CACHE_PATH = "GRID_CACHE_PATH";
    static final String GRID_CACHE_READER_COUNT = "GRID_CACHE_READER_COUNT";
    static final String LAYER_PROPERTIES = "layer.properties";
    private FieldDAO fieldDao;
    private LayerDAO layerDao;
    String layerFilesPath;
    String alaspatialOutputPath;
    String layerIndexUrl;
    int batchThreadCount;
    long configReloadWait;
    long lastReload;
    String preloadedShapeFiles;
    int gridBufferSize;
    SimpleShapeFileCache shapeFileCache;
    HashMap<String, IntersectionFile> intersectionFiles;
    String gridCachePath;
    int gridCacheReaderCount;

    public IntersectConfig(FieldDAO fieldDao, LayerDAO layerDao) {
        this.fieldDao = fieldDao;
        this.layerDao = layerDao;

        load();
    }

    public void load() {
        if (lastReload + configReloadWait >= System.currentTimeMillis()) {
            return;
        }
        lastReload = System.currentTimeMillis();

        Properties properties = new Properties();
        try {
            InputStream is = IntersectConfig.class.getResourceAsStream("/" + LAYER_PROPERTIES);
            if (is != null) {
                properties.load(is);
            } else {
                String msg = "cannot get properties file: " + IntersectConfig.class.getResource(LAYER_PROPERTIES).getFile();
                logger.warn(msg);
            }
        } catch (IOException ex) {
            logger.error(null, ex);
        }

        layerFilesPath = getProperty(LAYER_FILES_PATH, properties);
        alaspatialOutputPath = getProperty(ALASPATIAL_OUTPUT_PATH, properties);
        layerIndexUrl = getProperty(LAYER_INDEX_URL, properties);
        batchThreadCount = (int) getPositiveLongProperty(BATCH_THREAD_COUNT, properties, 1);
        configReloadWait = getPositiveLongProperty(CONFIG_RELOAD_WAIT, properties, 3600000);
        preloadedShapeFiles = getProperty(PRELOADED_SHAPE_FILES, properties);
        gridBufferSize = (int) getPositiveLongProperty(GRID_BUFFER_SIZE, properties, 4096);
        gridCachePath = getProperty(GRID_CACHE_PATH, properties);
        gridCacheReaderCount = (int) getPositiveLongProperty(GRID_CACHE_READER_COUNT, properties, 10);

        try {
            updateIntersectionFiles();
            updateShapeFileCache();
        } catch (Exception e) {
            //if it fails, set reload wait low
            logger.error("load failed, retry in 30s", e);
            configReloadWait = 30000;
        }
    }

    String getProperty(String property, Properties properties) {
        String p = System.getProperty(property);
        if (p == null) {
            p = properties.getProperty(property);
        }
        logger.info(property + " > " + p);
        return p;
    }

    long getPositiveLongProperty(String property, Properties properties, long defaultValue) {
        String p = getProperty(property, properties);
        long l = defaultValue;
        try {
            l = Long.parseLong(p);
            if (l < 0) {
                l = defaultValue;
            }
        } catch (NumberFormatException ex) {
            logger.error("parsing " + property + ": " + p + ", using default: " + defaultValue, ex);
        }
        return l;
    }

    public String getAlaspatialOutputPath() {
        return alaspatialOutputPath;
    }

    public String getLayerFilesPath() {
        return layerFilesPath;
    }

    public String getLayerIndexUrl() {
        return layerIndexUrl;
    }

    public int getThreadCount() {
        return batchThreadCount;
    }

    public IntersectionFile getIntersectionFile(String fieldId) {
        return intersectionFiles.get(fieldId);
    }

    public String getFieldIdFromFile(String file) {
        String off, on;
        if(File.separator.equals("/")) {
            off = "\\";
            on = "/";
        } else {
            on = "\\";
            off = "/";
        }
        file = file.replace(off, on);
        for(Entry<String, IntersectionFile> entry : intersectionFiles.entrySet()) {            
            if(entry.getValue().getFilePath().replace(off, on).equalsIgnoreCase(file)) {
                return entry.getKey();
            }
        }
        return file;
    }

    private void updateIntersectionFiles() throws MalformedURLException, IOException {
        if (intersectionFiles == null) {
            intersectionFiles = new HashMap<String, IntersectionFile>();
        }

        if (layerIndexUrl != null) {
            //request from url
            JSONArray layers = JSONArray.fromObject(getUrl(layerIndexUrl + "/layers"));
            HashMap<String, String> layerPathOrig = new HashMap<String, String>();
            HashMap<String, String> layerName = new HashMap<String, String>();
            for (int i = 0; i < layers.size(); i++) {
                layerPathOrig.put(layers.getJSONObject(i).getString("id"),
                        layers.getJSONObject(i).getString("path_orig"));
                layerPathOrig.put(layers.getJSONObject(i).getString("id"),
                        layers.getJSONObject(i).getString("name"));
            }

            JSONArray fields = JSONArray.fromObject(getUrl(layerIndexUrl + "/fieldsdb"));
            for (int i = 0; i < fields.size(); i++) {
                JSONObject jo = fields.getJSONObject(i);
                intersectionFiles.put(jo.getString("id"),
                        new IntersectionFile(jo.getString("name"),
                        layerFilesPath + layerPathOrig.get(jo.getString("spid")),
                        (jo.containsKey("sname") ? jo.getString("sname") : null),
                        layerName.get(jo.getString("spid")),
                        jo.getString("id")));
                //also register it under the layer name
                intersectionFiles.put(layerName.get(jo.getString("spid")),
                        new IntersectionFile(jo.getString("name"),
                        layerFilesPath + layerPathOrig.get(jo.getString("spid")),
                        (jo.containsKey("sname") ? jo.getString("sname") : null),
                        layerName.get(jo.getString("spid")),
                        jo.getString("id")));
            }
        } else {
            for (Field f : fieldDao.getFields()) {
                if (f.isIndb()) {
                    Layer layer = layerDao.getLayerById(Integer.parseInt(f.getSpid()));
                    intersectionFiles.put(f.getId(),
                            new IntersectionFile(f.getName(),
                            getLayerFilesPath() + layer.getPath_orig(),
                            f.getSname(),
                            layer.getName(),
                            f.getId()));
                }
            }
        }
    }

    String getUrl(String url) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);

            int result = client.executeMethod(get);
            String slist = get.getResponseBodyAsString();
            return slist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    void updateShapeFileCache() {
        if (preloadedShapeFiles == null) {
            return;
        }

        String[] fields = preloadedShapeFiles.split(",");

        //requres readLayerInfo() first
        String[] layers = new String[fields.length];
        String[] columns = new String[fields.length];
        String[] fid = new String[fields.length];
        if (fields.length == 1 && fields[0].equalsIgnoreCase("all")) {
            int countCL = 0;
            for (String s : intersectionFiles.keySet()) {
                if (s.startsWith("cl")) {
                    countCL++;
                }
            }
            layers = new String[countCL];
            columns = new String[countCL];
            fid = new String[countCL];
            int i = 0;
            for (IntersectionFile f : intersectionFiles.values()) {
                if (f.getFieldId().startsWith("cl")) {
                    layers[i] = f.getFilePath();
                    columns[i] = f.getShapeFields();
                    fid[i] = f.getFieldId();
                    i++;
                }
            }
        } else {
            for (int i = 0; i < fields.length; i++) {
                layers[i] = getIntersectionFile(fields[i].trim()).getFilePath();
                columns[i] = getIntersectionFile(fields[i].trim()).getShapeFields();
                fid[i] = fields[i];
            }
        }

        if (shapeFileCache == null) {
            shapeFileCache = new SimpleShapeFileCache(layers, columns, fid);
        } else {
            shapeFileCache.update(layers, columns, fid);
        }
    }

    /**
     * Add shape files to the shape file cache.
     *
     * @param fieldIds comma separated fieldIds.  Must be cl fields.
     */
    public void addToShapeFileCache(String fieldIds) {
        if(preloadedShapeFiles != null) {
            fieldIds += "," + preloadedShapeFiles;
        }
        String[] fields = fieldIds.split(",");

        //requres readLayerInfo() first
        String[] layers = new String[fields.length];
        String[] columns = new String[fields.length];
        String[] fid = new String[fields.length];

        int pos = 0;
        for (int i = 0; i < fields.length; i++) {
            try {
                layers[pos] = getIntersectionFile(fields[i].trim()).getFilePath();
                columns[pos] = getIntersectionFile(fields[i].trim()).getShapeFields();
                fid[pos] = fields[i];
                pos++;
            } catch (Exception e) {
                logger.error("problem adding shape file to cache for field: " + fields[i], e);
            }
        }
        if(pos < layers.length) {
            layers = java.util.Arrays.copyOf(layers, pos);
            columns = java.util.Arrays.copyOf(columns, pos);
            fid = java.util.Arrays.copyOf(fid, pos);
        }

        if (shapeFileCache == null) {
            shapeFileCache = new SimpleShapeFileCache(layers, columns, fid);
        } else {
            shapeFileCache.update(layers, columns, fid);
        }
    }

    public SimpleShapeFileCache getShapeFileCache() {
        return shapeFileCache;
    }

    public int getGridBufferSize() {
        return gridBufferSize;
    }

    public String getGridCachePath() {
        return gridCachePath;
    }

    public int getGridCacheReaderCount() {
        return gridCacheReaderCount;
    }
}