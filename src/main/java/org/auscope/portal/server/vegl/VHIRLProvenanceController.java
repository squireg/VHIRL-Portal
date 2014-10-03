package org.auscope.portal.server.vegl;

import au.csiro.promsclient.Activity;
import au.csiro.promsclient.Entity;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.auscope.portal.core.cloud.CloudFileInformation;
import org.auscope.portal.core.server.PortalPropertyPlaceholderConfigurer;
import org.auscope.portal.core.services.PortalServiceException;
import org.auscope.portal.core.services.cloud.CloudComputeService;
import org.auscope.portal.core.services.cloud.CloudStorageService;
import org.auscope.portal.core.services.cloud.FileStagingService;
import org.auscope.portal.server.web.controllers.BaseCloudController;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by wis056 on 3/10/2014.
 */
public class VHIRLProvenanceController extends BaseCloudController {
    /** Logger for this class */
    private static final Log logger = LogFactory.getLog(VHIRLProvenanceController.class);
    private static final String activityFileName = "activity.ttl";
    private String serverURL = null;
    private static final String PROV = "http://www.w3.org/ns/prov#";

    private VHIRLFileStagingService fileStagingService;

    @Autowired
    public VHIRLProvenanceController(VHIRLFileStagingService fileStagingService, CloudStorageService[] cloudStorageServices, CloudComputeService[] cloudComputeServices, PortalPropertyPlaceholderConfigurer hostConfigurer) {
        super(cloudStorageServices, cloudComputeServices, hostConfigurer);
        this.fileStagingService = fileStagingService;
    }

    public void createActivity(VEGLJob job, String serverURL) {
        if(this.serverURL == null || ! this.serverURL.equals(serverURL))
            this.serverURL = serverURL;
        String jobURL = jobURL(job);
        Activity vhirlJob = null;
        ArrayList<Entity> inputs = createEntitiesForInputs(job);
        try {
            vhirlJob = new Activity(new URI(jobURL), job.getName(), job.getDescription(), serverURL, new Date(), null, inputs, null);
        } catch (URISyntaxException ex) {
            logger.error(String.format("Error parsing server name %s into URI.", jobURL), ex);
        }
        uploadModel(vhirlJob.get_graph(), job);
    }

    protected void uploadModel(Model model, VEGLJob job) {
        try {
            File tmpActivity = fileStagingService.createLocalFile(activityFileName, job);
//            File tmpActivity = File.createTempFile("activity", ".turtle");
            FileWriter fileWriter = new FileWriter(tmpActivity);
            model.write(fileWriter, "TURTLE");
            File[] files = {tmpActivity};

            CloudStorageService cloudStorageService = getStorageService(job);
            cloudStorageService.uploadJobFiles(job, files);
        } catch (IOException | PortalServiceException e) {
            // JAVA RAGE
        }
    }

    protected String jobURL(VEGLJob job) {
        return String.format("%s/getJobObject.do?jobId=%s", serverURL, job.getId());

    }

    protected String outputURL(VEGLJob job, CloudFileInformation outputInfo) {
        return String.format("%s/secure/jobFile.do?jobId=%s&key=%s", serverURL, job.getId(), outputInfo.getCloudKey());
    }

    public static ArrayList<Entity> createEntitiesForInputs(VEGLJob job) {
        ArrayList<Entity> inputs = new ArrayList<>();
        try {
            for (VglDownload dataset : job.getJobDownloads()) {
                inputs.add(new Entity(new URI(dataset.getUrl())));
            }
        } catch (URISyntaxException ex) {
            logger.error(String.format("Error parsing data source urls %s into URIs.", job.getJobDownloads().toString()), ex);
        }
        return inputs;
    }

    public void createEntitiesForOutputs(VEGLJob job) {
        ArrayList<Entity> outputs = new ArrayList<>();
        CloudStorageService cloudStorageService = getStorageService(job);
        CloudFileInformation[] fileInformations = null;
        Model activity = null;
        Resource resource = null;
        try {
            fileInformations = cloudStorageService.listJobFiles(job);
            for (CloudFileInformation information : fileInformations) {
                List<VglDownload> inputs =  job.getJobDownloads();
                List<String> names = new ArrayList<>();
                for (VglDownload input : inputs) {
                    names.add(input.getName());
                }
                if (information.getName().equals(activityFileName)) {
                    // Here's our Turtle!
                    InputStream activityStream = cloudStorageService.getJobFile(job, activityFileName);
                    activity = ModelFactory.createDefaultModel().read(activityStream, "TURTLE");
                    resource = activity.getResource(jobURL(job));

                } else if (names.contains(information.getName())) {
                    // This is an input, do nothing.
                } else {
                    // Ah ha! This must be an output.
                    outputs.add(new Entity(new URI(outputURL(job, information))));
                }
            }
        } catch (PortalServiceException | URISyntaxException ex) {
            logger.error(String.format("Error parsing data results urls %s into URIs.", job.getJobDownloads().toString()), ex);
        }
        for (Entity entity : outputs) {
            resource.addLiteral(activity.createProperty(PROV + "generated"), activity.createTypedLiteral(entity.get_id().toString()));
            activity.add(entity.get_graph());
        }

        uploadModel(activity, job);
    }
}
