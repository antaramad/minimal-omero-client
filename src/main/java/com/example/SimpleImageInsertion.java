package com.example;

import com.CustomMetadata;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import ome.formats.importer.cli.LoggingImportMonitor;
import omero.RLong;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.MapAnnotationData;
import omero.gateway.model.ProjectData;
import omero.model.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;


public class SimpleImageInsertion {
    /** Reference to the gateway.*/
    private Gateway gateway;

    /** The security context.*/
    private SecurityContext ctx;

    public Gateway getGateway() {
        return gateway;
    }

    public SecurityContext getCtx() {
        return ctx;
    }

    public SimpleImageInsertion(Gateway gateway, SecurityContext ctx) {
        this.ctx = ctx;
        this.gateway= gateway;
    }

    private RLong createDataset(long projectId) {
        IObject r = null;

        try {
           DataManagerFacility dm = gateway.getFacility(DataManagerFacility.class);
           IObject datasetCreated = null;

           // Create a dataset and link it to an existing project and Using the pojo
            /*DatasetData datasetData = new DatasetData();
            datasetData.setName("new Name 2");
            datasetData.setDescription("new description 2");
            BrowseFacility browser = gateway.getFacility(BrowseFacility.class);
            ProjectData projectData = browser.getProjects(ctx, Collections.singleton(projectId)).iterator().next();
            datasetData.setProjects(Collections.singleton(projectData));
            datasetCreated = dm.saveAndReturnObject(ctx, datasetData).asIObject();*/

           //Using IObject directly
           Dataset dataset = new DatasetI();
           dataset.setName(omero.rtypes.rstring("new Name 1"));
           dataset.setDescription(omero.rtypes.rstring("new description 1"));
           ProjectDatasetLink link = new ProjectDatasetLinkI();
           link.setChild(dataset);
           link.setParent(new ProjectI(projectId, false));
           r = dm.saveAndReturnObject(ctx, link);
       } catch (ExecutionException | DSOutOfServiceException | DSAccessException e){
                e.printStackTrace();
        }

        return r.getId();
    }

public static void main(String[] args){
    ImportConfig config = new ome.formats.importer.ImportConfig();
    config.email.set("");
    config.sendFiles.set(true);
    config.sendReport.set(false);
    config.contOnError.set(false);
    config.debug.set(false);

    config.hostname.set("localhost");
    config.port.set(4064);
    config.username.set("adufour");
    config.password.set("adufour");

    // the imported image will go into 'orphaned images' unless
    // you specify a particular existing dataset like this:
    // config.target.set("Dataset:123");

    SimpleConnection clientConnection = new SimpleConnection();

    try {

        // To delete
        long datasetId = Long.valueOf(619);

        clientConnection.connect(config);
        SimpleImageInsertion imageInsertion = new SimpleImageInsertion(clientConnection.getGateway(), clientConnection.getCtx());
        DatasetData dataset = imageInsertion.getDatasetData(datasetId);

        // Get directory path
        Path dirPath = Paths.get(args[0]);
        String inputPath = dirPath.toString();

        CustomMetadata customMetadata = new CustomMetadata();

        if (inputPath.equals("")) {
            System.err.println("Please choose an input folder");
            return;
        }

        File f1 = new File(inputPath);
        if (!f1.exists() || !f1.isDirectory()) {
            System.err.println("Input does not exist or is not a folder\n \n"+inputPath);
            return;
        }

        String[] list = (new File(inputPath)).list();
        Map<String, String> metadataNameAndValue = customMetadata.getCustomTiffMetadata(inputPath + "\\"+ list[0]);


        // 4 - Metadata added to the parent dataset
        List<NamedValue> result = new ArrayList<>();
        for(Map.Entry<String, String> data : metadataNameAndValue.entrySet()) {
            result.add(new NamedValue(data.getKey(), data.getValue()));
        }

        MapAnnotationData mapAnnotationData = new MapAnnotationData();
        mapAnnotationData.setContent(result);

        //Use the following namespace if you want the annotation to be editable
        //in the webclient and insight
        mapAnnotationData.setNameSpace(MapAnnotationData.NS_CLIENT_CREATED);
        DataManagerFacility fac = imageInsertion.getGateway().getFacility(DataManagerFacility.class);
        fac.attachAnnotation(imageInsertion.getCtx(), mapAnnotationData, dataset);

    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        clientConnection.disconnect();
    }

}

    private DatasetData getDatasetData(long datasetId) throws ExecutionException, DSOutOfServiceException, DSAccessException {
        BrowseFacility browser = this.getGateway().getFacility(BrowseFacility.class);
        DatasetData dataset = browser.getDatasets(this.getCtx(), Collections.singleton(Long.valueOf(datasetId))).iterator().next();
        return dataset;
    }
}
