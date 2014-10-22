package org.auscope.portal.server.web.service;

import au.csiro.promsclient.Activity;
import au.csiro.promsclient.Entity;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.auscope.portal.core.cloud.CloudFileInformation;
import org.auscope.portal.core.services.PortalServiceException;
import org.auscope.portal.core.services.cloud.CloudStorageService;
import org.auscope.portal.server.vegl.VEGLJob;
import org.auscope.portal.server.vegl.VglDownload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by wis056 on 3/10/2014.
 */
@Service
public class VHIRLProvenanceService {
    /** Logger for this class. */
    private static final Log LOGGER = LogFactory.getLog(VHIRLProvenanceService
            .class);
    /** Default name for the half-baked provenance uploaded to the cloud. */
    private static final String ACTIVITY_FILE_NAME = "activity.ttl";
    /** Document type for output. */
    private static final String TURTLE_FORMAT = "TTL";
    /** URL of the current webserver. Will need to be set by classes
     * using this service. */
    private String serverURL = null;
    /** The PROV prefix for some fiddling. */
    private static final String PROV = "http://www.w3.org/ns/prov#";
    /** The service to allow us to write temporary local files. */
    private VHIRLFileStagingService vhirlFileStagingService;
    /** The service to allow us to write files to the cloud. */
    private CloudStorageService[] cloudStorageServices;

    /**
     * Autowired constructor for Spring -- don't use this directly, you should
     * be able to autowire this into your own class.
     * @param newVhirlFileStagingService set the local file store must not
     *                                   be null
     * @param newCloudStorageServices set the cloud file store must not be null
     */
    @Autowired
    public VHIRLProvenanceService(final VHIRLFileStagingService
                                              newVhirlFileStagingService,
                                  final CloudStorageService[]
                                          newCloudStorageServices) {
        this.vhirlFileStagingService = newVhirlFileStagingService;
        this.cloudStorageServices = newCloudStorageServices;

    }

    /**
     * Create the half-baked provenance information for this job just before it
     * starts. This will create the provenance information on the inputs, job
     * and script, but not on the outputs (as they don't exist yet).
     * @param job The Virtual Labs Job we want to report provenance on. It
     *            should be just about to execute, but not yet have started.
     * @param newServerURL The URL of the current webserver. Can be null.
     * @return The TURTLE text.
     */
    public final String createActivity(final VEGLJob job, final String
            newServerURL) {
        if (this.serverURL == null || !this.serverURL.equals(newServerURL)) {
            this.serverURL = newServerURL;
        }
        String jobURL = jobURL(job, serverURL);
        Activity vhirlJob = null;
        ArrayList<Entity> inputs = createEntitiesForInputs(job);
        try {
            vhirlJob = new Activity()
                    .setActivityUri(new URI(jobURL))
                    .setTitle(job.getName())
                    .setDescription(job.getDescription())
                    .setWasAttributedTo(serverURL)
                    .setStartedAtTime(new Date())
                    .setUsedEntities(inputs);
        } catch (URISyntaxException ex) {
            LOGGER.error(String.format("Error parsing server name %s into URI.",
                    jobURL), ex);
        }
        uploadModel(vhirlJob.getGraph(), job);
        StringWriter out = new StringWriter();
        vhirlJob.getGraph().write(out, TURTLE_FORMAT);
        return out.toString();
    }

    /**
     * Upload a complete or partially complete model to the cloud for storage.
     * @param model The RDF model to serialize and upload to the cloud.
     * @param job The virtual lab job this model refers to.
     */
    protected final void uploadModel(final Model model, final VEGLJob job) {
        if (model != null) {
            try {
                File tmpActivity = vhirlFileStagingService.createLocalFile(
                        ACTIVITY_FILE_NAME, job);
                FileWriter fileWriter = new FileWriter(tmpActivity);
                model.write(fileWriter, TURTLE_FORMAT);
                fileWriter.close();
                File[] files = {tmpActivity};

                CloudStorageService cloudStorageService =
                        getStorageService(job);
                cloudStorageService.uploadJobFiles(job, files);
            } catch (IOException | PortalServiceException e) {
                // JAVA RAGE
                LOGGER.error(e.getLocalizedMessage());
            }
        }
    }

    /**
     * Looks through the list of all cloud storage providers and finds one we
     * can use for this job.
     * @param job The virtual lab job we want to know the appropriate cloud
     *            providers for.
     * @return The first cloud provider selected for this job, or null if none
     * has yet been assigned.
     */
    protected final CloudStorageService getStorageService(final VEGLJob job) {
        for (CloudStorageService s : cloudStorageServices) {
            if (s.getId().equals(job.getStorageServiceId())) {
                return s;
            }
        }
        return null;
    }

    /**
     * Constructs a full URL which can be used to get information (JSON) about
     * a job.
     * @param job The virtual labs job we want a url for.
     * @param serverURL URL of the webserver.
     * @return The URL for this job.
     */
    protected static String jobURL(final VEGLJob job, final String serverURL) {
        return String.format("%s/getJobObject.do?jobId=%s", serverURL,
                job.getId());
    }

    /**
     * Get a unique url for this output file.
     * @param job The virtual labs job this output belongs to.
     * @param outputInfo The metadata for the output file.
     * @param serverURL URL of the webserver.
     * @return A URL for the file. May or may not be public.
     */
    protected static String outputURL(final VEGLJob job,
                                     final CloudFileInformation outputInfo,
                                     final String serverURL) {
        return String.format("%s/secure/jobFile.do?jobId=%s&key=%s", serverURL,
                job.getId(), outputInfo.getCloudKey());
    }

    /**
     * Looks through the input files listed for a job and create appropriate
     * PROV-O Entities for them.
     * @param job The virtual labs job we want to examine the inputs of.
     * @return An array of PROV-O entities. May be empty, but won't be null.
     */
    public static ArrayList<Entity> createEntitiesForInputs(final VEGLJob job) {
        ArrayList<Entity> inputs = new ArrayList<>();
        try {
            for (VglDownload dataset : job.getJobDownloads()) {
                inputs.add(new Entity(new URI(dataset.getUrl())));
            }
        } catch (URISyntaxException ex) {
            LOGGER.error(String.format(
                    "Error parsing data source urls %s into URIs.",
                    job.getJobDownloads().toString()), ex);
        }
        return inputs;
    }

    /**
     * Takes a completed job and finishes creating the provenance record, and
     * uploads it to the cloud. The job *must* have had
     * {@link #createActivity(org.auscope.portal.server.vegl.VEGLJob, String)}
     * called with it already. Otherwise it can't collect the relevant
     * information, and won't do anything.
     * @param job Completed virtual labs job, about which we will finish our
     *            provenance gathering.
     */
    public final String createEntitiesForOutputs(final VEGLJob job) {
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
                if (information.getName().equals(ACTIVITY_FILE_NAME)) {
                    // Here's our Turtle!
                    InputStream activityStream =
                            cloudStorageService.getJobFile(job,
                                    ACTIVITY_FILE_NAME);
                    activity = ModelFactory.createDefaultModel();
                    activity.read(activityStream, serverURL, TURTLE_FORMAT);
                    resource = activity.getResource(jobURL(job, serverURL));
                    LOGGER.info(activity);
                    LOGGER.info(resource);

                } else if (names.contains(information.getName())) {
                    // This is an input, do nothing.
                    continue;
                } else {
                    // Ah ha! This must be an output.
                    outputs.add(new Entity(new URI(outputURL(
                            job, information, serverURL))));
                }
            }
        } catch (PortalServiceException | URISyntaxException ex) {
            LOGGER.error(String.format(
                    "Error parsing data results urls %s into URIs.",
                    job.getJobDownloads().toString()), ex);
        }
        for (Entity entity : outputs) {
            if (resource != null) {
                resource.addLiteral(activity.createProperty(PROV + "generated"),
                        activity.createTypedLiteral(entity.get_id().toString()));
                activity.add(entity.get_graph());
            }
        }

        if (activity != null) {
            uploadModel(activity, job);
            StringWriter out = new StringWriter();
            activity.write(out, TURTLE_FORMAT);
            return out.toString();
        } else {
            return "";
        }
    }
}
