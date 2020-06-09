/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Bosch Software Innovations - added Redis URL support with authentication
 *     Firis SA - added mDNS services registering 
 *******************************************************************************/
package org.eclipse.leshan.server.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.UUID;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.core.LwM2m;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.demo.servlet.ClientServlet;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.ObjectSpecServlet;
import org.eclipse.leshan.server.demo.servlet.SecurityServlet;
import org.eclipse.leshan.server.demo.utils.MagicLwM2mValueConverter;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.FileSecurityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class LeshanServerDemo {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServerDemo.class);

    // /!\ This field is a COPY of org.eclipse.leshan.client.demo.LeshanClientDemo.modelPaths /!\
    // TODO create a leshan-demo project ?
    private final static String[] modelPaths = new String[] { "10241.xml", "10242.xml", "10243.xml", "10244.xml",
                            "10245.xml", "10246.xml", "10247.xml", "10248.xml", "10249.xml", "10250.xml", "10251.xml",
                            "10252.xml", "10253.xml", "10254.xml", "10255.xml", "10256.xml", "10257.xml", "10258.xml",
                            "10259.xml", "10260-2_0.xml", "10260.xml", "10262.xml", "10263.xml", "10264.xml",
                            "10265.xml", "10266.xml", "10267.xml", "10268.xml", "10269.xml", "10270.xml", "10271.xml",
                            "10272.xml", "10273.xml", "10274.xml", "10275.xml", "10276.xml", "10277.xml", "10278.xml",
                            "10279.xml", "10280.xml", "10281.xml", "10282.xml", "10283.xml", "10284.xml", "10286.xml",
                            "10290.xml", "10291.xml", "10292.xml", "10299.xml", "10300.xml", "10308-2_0.xml",
                            "10308.xml", "10309.xml", "10311.xml", "10313.xml", "10314.xml", "10315.xml", "10316.xml",
                            "10318.xml", "10319.xml", "10320.xml", "10322.xml", "10323.xml", "10324.xml", "10326.xml",
                            "10327.xml", "10328.xml", "10329.xml", "10330.xml", "10331.xml", "10332.xml", "10333.xml",
                            "10334.xml", "10335.xml", "10336.xml", "10337.xml", "10338.xml", "10339.xml", "10340.xml",
                            "10341.xml", "10342.xml", "10343.xml", "10344.xml", "10345.xml", "10346.xml", "10347.xml",
                            "10348.xml", "10349.xml", "10350.xml", "10351.xml", "10352.xml", "10353.xml", "10354.xml",
                            "10355.xml", "10356.xml", "10357.xml", "10358.xml", "10359.xml", "10360.xml", "10361.xml",
                            "10362.xml", "10363.xml", "10364.xml", "10365.xml", "10366.xml", "10368.xml", "10369.xml",

                            "2048.xml", "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml", "2054.xml",
                            "2055.xml", "2056.xml", "2057.xml",

                            "3200.xml", "3201.xml", "3202.xml", "3203.xml", "3300.xml", "3301.xml", "3302.xml",
                            "3303.xml", "3304.xml", "3305.xml", "3306.xml", "3308.xml", "3310.xml", "3311.xml",
                            "3312.xml", "3313.xml", "3314.xml", "3315.xml", "3316.xml", "3317.xml", "3318.xml",
                            "3319.xml", "3320.xml", "3321.xml", "3322.xml", "3323.xml", "3324.xml", "3325.xml",
                            "3326.xml", "3327.xml", "3328.xml", "3329.xml", "3330.xml", "3331.xml", "3332.xml",
                            "3333.xml", "3334.xml", "3335.xml", "3336.xml", "3337.xml", "3338.xml", "3339.xml",
                            "3340.xml", "3341.xml", "3342.xml", "3343.xml", "3344.xml", "3345.xml", "3346.xml",
                            "3347.xml", "3348.xml", "3349.xml", "3350.xml", "3351.xml", "3352.xml", "3353.xml",
                            "3354.xml", "3355.xml", "3356.xml", "3357.xml", "3358.xml", "3359.xml", "3360.xml",
                            "3361.xml", "3362.xml", "3363.xml", "3364.xml", "3365.xml", "3366.xml", "3367.xml",
                            "3368.xml", "3369.xml", "3370.xml", "3371.xml", "3372.xml", "3373.xml", "3374.xml",
                            "3375.xml", "3376.xml", "3377.xml", "3378.xml", "3379.xml", "3380-2_0.xml", "3380.xml",
                            "3381.xml", "3382.xml", "3383.xml", "3384.xml", "3385.xml", "3386.xml",

                            "LWM2M_APN_Connection_Profile-v1_0_1.xml", "LWM2M_Bearer_Selection-v1_0_1.xml",
                            "LWM2M_Cellular_Connectivity-v1_0_1.xml", "LWM2M_DevCapMgmt-v1_0.xml",
                            "LWM2M_LOCKWIPE-v1_0_1.xml", "LWM2M_Portfolio-v1_0.xml",
                            "LWM2M_Software_Component-v1_0.xml", "LWM2M_Software_Management-v1_0.xml",
                            "LWM2M_WLAN_connectivity4-v1_0.xml", "LwM2M_BinaryAppDataContainer-v1_0_1.xml",
                            "LwM2M_EventLog-V1_0.xml" };

    private final static String USAGE = "java -jar leshan-server-demo.jar [OPTION]\n\n";

    private final static AmazonSQS sqs = AmazonSQSClientBuilder.standard()
            .withRegion(System.getenv("AWS_REGION"))
            .withCredentials(
                    new AWSStaticCredentialsProvider(
                            new AWSCredentials() {
                                @Override
                                public String getAWSAccessKeyId() {
                                    return System.getenv("AWS_ACCESS_KEY_ID");
                                }

                                @Override
                                public String getAWSSecretKey() {
                                    return System.getenv("AWS_SECRET_ACCESS_KEY");
                                }
                            }
                    )
            ).build();

    public static void main(String[] args) throws FileNotFoundException {
        // Define options for command line tools
        Options options = new Options();

        final StringBuilder RPKChapter = new StringBuilder();
        RPKChapter.append("\n .");
        RPKChapter.append("\n .");
        RPKChapter.append("\n================================[ RPK ]=================================");
        RPKChapter.append("\n| By default Leshan demo uses an embedded self-signed certificate and  |");
        RPKChapter.append("\n| trusts any client certificates allowing to use RPK or X509           |");
        RPKChapter.append("\n| at client side.                                                      |");
        RPKChapter.append("\n| To use RPK only with your own keys :                                 |");
        RPKChapter.append("\n|            -pubk -prik options should be used together.              |");
        RPKChapter.append("\n| To get helps about files format and how to generate it, see :        |");
        RPKChapter.append("\n| See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        RPKChapter.append("\n------------------------------------------------------------------------");

        final StringBuilder X509Chapter = new StringBuilder();
        X509Chapter.append("\n .");
        X509Chapter.append("\n .");
        X509Chapter.append("\n===============================[ X509 ]=================================");
        X509Chapter.append("\n| By default Leshan demo uses an embedded self-signed certificate and  |");
        X509Chapter.append("\n| trusts any client certificates allowing to use RPK or X509           |");
        X509Chapter.append("\n| at client side.                                                      |");
        X509Chapter.append("\n| To use X509 with your own server key, certificate and truststore :   |");
        X509Chapter.append("\n|               [-cert, -prik], [-truststore] should be used together  |");
        X509Chapter.append("\n| To get helps about files format and how to generate it, see :        |");
        X509Chapter.append("\n| See https://github.com/eclipse/leshan/wiki/Credential-files-format   |");
        X509Chapter.append("\n------------------------------------------------------------------------");

        options.addOption("h", "help", false, "Display help information.");
        options.addOption("lh", "coaphost", true, "Set the local CoAP address.\n  Default: any local address.");
        options.addOption("lp", "coapport", true,
                String.format("Set the local CoAP port.\n  Default: %d.", LwM2m.DEFAULT_COAP_PORT));
        options.addOption("slh", "coapshost", true, "Set the secure local CoAP address.\nDefault: any local address.");
        options.addOption("slp", "coapsport", true,
                String.format("Set the secure local CoAP port.\nDefault: %d.", LwM2m.DEFAULT_COAP_SECURE_PORT));
        options.addOption("wh", "webhost", true, "Set the HTTP address for web server.\nDefault: any local address.");
        options.addOption("wp", "webport", true, "Set the HTTP port for web server.\nDefault: 8080.");
        options.addOption("m", "modelsfolder", true, "A folder which contains object models in OMA DDF(.xml) format.");
        options.addOption("oc", "activate support of old/deprecated cipher suites.");
        options.addOption("pubk", true,
                "The path to your server public key file.\n The public Key should be in SubjectPublicKeyInfo format (DER encoding).");
        options.addOption("prik", true,
                "The path to your server private key file.\nThe private key should be in PKCS#8 format (DER encoding)."
                        + X509Chapter);
        options.addOption("cert", true,
                "The path to your server certificate file.\n"
                        + "The certificate Common Name (CN) should generally be equal to the server hostname.\n"
                        + "The certificate should be in X509v3 format (DER encoding).");

        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.setOptionComparator(null);

        // Parse arguments
        CommandLine cl;
        try {
            cl = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Print help
        if (cl.hasOption("help")) {
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if unexpected options
        if (cl.getArgs().length > 0) {
            System.err.println("Unexpected option or arguments : " + cl.getArgList());
            formatter.printHelp(USAGE, options);
            return;
        }

        // Abort if all RPK config is not complete
        boolean rpkConfig = false;
        if (cl.hasOption("pubk")) {
            if (!cl.hasOption("prik")) {
                System.err.println("pubk, prik should be used together to connect using RPK");
                formatter.printHelp(USAGE, options);
                return;
            } else {
                rpkConfig = true;
            }
        }

        // Abort if all X509 config is not complete
        boolean x509Config = false;
        if (cl.hasOption("cert")) {
            if (!cl.hasOption("prik")) {
                System.err.println("cert, prik should be used together to connect using X509");
                formatter.printHelp(USAGE, options);
                return;
            } else {
                x509Config = true;
            }
        }

        // Abort if prik is used without complete RPK or X509 config
        if (cl.hasOption("prik")) {
            if (!rpkConfig && !x509Config) {
                System.err.println("prik should be used with cert for X509 config OR pubk for RPK config");
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // get local address
        String localAddress = cl.getOptionValue("lh");
        String localPortOption = cl.getOptionValue("lp");
        Integer localPort = null;
        if (localPortOption != null) {
            localPort = Integer.parseInt(localPortOption);
        }

        // get secure local address
        String secureLocalAddress = cl.getOptionValue("slh");
        String secureLocalPortOption = cl.getOptionValue("slp");
        Integer secureLocalPort = null;
        if (secureLocalPortOption != null) {
            secureLocalPort = Integer.parseInt(secureLocalPortOption);
        }

        // get http address
        String webAddress = cl.getOptionValue("wh");
        String webPortOption = cl.getOptionValue("wp");
        int webPort = 8080;
        if (webPortOption != null) {
            webPort = Integer.parseInt(webPortOption);
        }

        // Get models folder
        String modelsFolderPath = cl.getOptionValue("m");

        // get RPK info
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        if (rpkConfig) {
            try {
                privateKey = SecurityUtil.privateKey.readFromFile(cl.getOptionValue("prik"));
                publicKey = SecurityUtil.publicKey.readFromFile(cl.getOptionValue("pubk"));
            } catch (Exception e) {
                System.err.println("Unable to load RPK files : " + e.getMessage());
                e.printStackTrace();
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // get X509 info
        X509Certificate certificate = null;
        if (cl.hasOption("cert")) {
            try {
                privateKey = SecurityUtil.privateKey.readFromFile(cl.getOptionValue("prik"));
                certificate = SecurityUtil.certificate.readFromFile(cl.getOptionValue("cert"));
            } catch (Exception e) {
                System.err.println("Unable to load X509 files : " + e.getMessage());
                e.printStackTrace();
                formatter.printHelp(USAGE, options);
                return;
            }
        }

        // get X509 info
        List<Certificate> trustStore = null;
        if (cl.hasOption("truststore")) {
            trustStore = new ArrayList<>();
            File input = new File(cl.getOptionValue("truststore"));

            // check input exists
            if (!input.exists())
                throw new FileNotFoundException(input.toString());

            // get input files.
            File[] files;
            if (input.isDirectory()) {
                files = input.listFiles();
            } else {
                files = new File[] { input };
            }
            for (File file : files) {
                try {
                    trustStore.add(SecurityUtil.certificate.readFromFile(file.getAbsolutePath()));
                } catch (Exception e) {
                    LOG.warn("Unable to load X509 files {}:{} ", file.getAbsolutePath(), e.getMessage());
                }
            }
        }

        try {
            createAndStartServer(webAddress, webPort, localAddress, localPort, secureLocalAddress, secureLocalPort,
                    modelsFolderPath, publicKey, privateKey, certificate, trustStore,cl.hasOption("oc"));
        } catch (BindException e) {
            System.err.println(
                    String.format("Web port %s is already used, you could change it using 'webport' option.", webPort));
            formatter.printHelp(USAGE, options);
        } catch (Exception e) {
            LOG.error("Jetty stopped with unexpected error ...", e);
        }
    }

    public static void createAndStartServer(String webAddress, int webPort, String localAddress, Integer localPort,
            String secureLocalAddress, Integer secureLocalPort, String modelsFolderPath,
            PublicKey publicKey, PrivateKey privateKey, X509Certificate certificate, List<Certificate> trustStore,boolean supportDeprecatedCiphers) throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        // Create CoAP Config
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }
        builder.setCoapConfig(coapConfig);

        // ports from CoAP Config if needed
        builder.setLocalAddress(localAddress,
                localPort == null ? coapConfig.getInt(Keys.COAP_PORT, LwM2m.DEFAULT_COAP_PORT) : localPort);
        builder.setLocalSecureAddress(secureLocalAddress,
                secureLocalPort == null ? coapConfig.getInt(Keys.COAP_SECURE_PORT, LwM2m.DEFAULT_COAP_SECURE_PORT)
                        : secureLocalPort);

        // Create DTLS Config
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(!supportDeprecatedCiphers);

        X509Certificate serverCertificate = null;
        if (certificate != null) {
            // use X.509 mode (+ RPK)
            serverCertificate = certificate;
            builder.setPrivateKey(privateKey);
            builder.setCertificateChain(new X509Certificate[] { serverCertificate });
        } else if (publicKey != null) {
            // use RPK only
            builder.setPublicKey(publicKey);
            builder.setPrivateKey(privateKey);
        }

        if (publicKey == null && serverCertificate == null) {
            // public key or server certificated is not defined
            // use default embedded credentials (X.509 + RPK mode)
            try {
                PrivateKey embeddedPrivateKey = SecurityUtil.privateKey
                        .readFromResource("credentials/server_privkey.der");
                serverCertificate = SecurityUtil.certificate.readFromResource("credentials/server_cert.der");
                builder.setPrivateKey(embeddedPrivateKey);
                builder.setCertificateChain(new X509Certificate[] { serverCertificate });
            } catch (Exception e) {
                LOG.error("Unable to load embedded X.509 certificate.", e);
                System.exit(-1);
            }
        }

        // Define trust store
        if (serverCertificate != null) {
            if (trustStore != null && !trustStore.isEmpty()) {
                builder.setTrustedCertificates(trustStore.toArray(new Certificate[trustStore.size()]));
            } else {
                // by default trust all
                builder.setTrustedCertificates(new X509Certificate[0]);
            }
        }

        // Set DTLS Config
        builder.setDtlsConfig(dtlsConfig);

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", modelPaths));
        if (modelsFolderPath != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(new File(modelsFolderPath)));
        }
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // Set securityStore & registrationStore
        // use file persistence
        EditableSecurityStore securityStore = new FileSecurityStore();
        builder.setSecurityStore(securityStore);

        // use a magic converter to support bad type send by the UI.
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new MagicLwM2mValueConverter()));

        // Create and start LWM2M server
        final LeshanServer lwServer = builder.build();

        // Now prepare Jetty
        InetSocketAddress jettyAddr;
        if (webAddress == null) {
            jettyAddr = new InetSocketAddress(webPort);
        } else {
            jettyAddr = new InetSocketAddress(webAddress, webPort);
        }
        Server server = new Server(jettyAddr);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(LeshanServerDemo.class.getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecuredAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder;
        if (publicKey != null) {
            securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, publicKey));
        } else {
            securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, serverCertificate));
        }
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(
                new ObjectSpecServlet(lwServer.getModelProvider(), lwServer.getRegistrationService()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        // Start Jetty & Leshan
        lwServer.start();
        server.start();
        LOG.info("Web server started at {}.", server.getURI());

        LOG.info("AWS Region: {}", System.getenv("AWS_REGION"));
        LOG.info("AWS Queue URL: {}", System.getenv("AWS_QUEUE_URL"));

        lwServer.getRegistrationService().addListener(new RegistrationListener() {
            @Override
            public void registered(Registration registration, Registration previousReg,
                                   Collection<Observation> previousObsersations) {
                System.out.println("new device: " + registration.getEndpoint());
                // Observe battery
                try {
                    ReadResponse response = lwServer.send(registration, new ObserveRequest(3, 0, 9));
                    if (response.isSuccess()) {
                        System.out.println("Device battery level: " + ((LwM2mResource)response.getContent()).getValue());
                        publishNotification(registration, 3,0,9, ((LwM2mResource)response.getContent()).getValue().toString());
                    }else {
                        System.out.println("Failed to read:" + response.getCode() + " " + response.getErrorMessage());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Observe temperature
                try {
                    ReadResponse response = lwServer.send(registration, new ObserveRequest(3303,0,5700));
                    if (response.isSuccess()) {
                        System.out.println("Device temperature: " + ((LwM2mResource)response.getContent()).getValue());
                        publishNotification(registration, 3303,0,5700, ((LwM2mResource)response.getContent()).getValue().toString());
                    }else {
                        System.out.println("Failed to read: " + response.getCode() + " " + response.getErrorMessage());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
                System.out.println("device is still here: " + updatedReg.getEndpoint());
            }

            @Override
            public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                                     Registration newReg) {
                System.out.println("device left: " + registration.getEndpoint());
            }
        });

        lwServer.getObservationService().addListener(new ObservationListener() {
            @Override
            public void newObservation(Observation observation, Registration registration) {
                System.out.println("new obervation: " + registration.getEndpoint());
            }

            @Override
            public void cancelled(Observation observation) {
                System.out.println("observation cancelled: " + observation.getRegistrationId());
            }

            @Override
            public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
                System.out.println("observation response: " + registration.getEndpoint() + " " + response.getCode() + " " + response.getContent());
                publishObservationResponse(registration, response);
            }

            @Override
            public void onError(Observation observation, Registration registration, Exception error) {
                System.out.println("observation error: " + observation.getRegistrationId());
                System.out.println(error.toString());
            }
        });

        lwServer.getPresenceService().addListener(new PresenceListener() {
            @Override
            public void onAwake(Registration registration) {
                System.out.println("onAwake: " + registration.getEndpoint());
            }

            @Override
            public void onSleeping(Registration registration) {
                System.out.println("onSleeping: " + registration.getEndpoint());
            }
        });


    }

    private static void publishObservationResponse (Registration registration, ObserveResponse response) {
        LwM2mPath path =response.getObservation().getPath();
        publishNotification(registration, path.getObjectId(), path.getObjectInstanceId(), path.getResourceId(), ((LwM2mSingleResource) response.getContent()).getValue().toString());
    }

    private static void publishNotification (Registration registration, int objectId, int objectInstanceId, int resourceId, String value) {
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        // Endpoint
        messageAttributes.put("Endpoint", new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(registration.getEndpoint()));

        // Path
        messageAttributes.put("objectId", new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(String.valueOf(objectId)));
        messageAttributes.put("objectInstanceId", new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(String.valueOf(objectInstanceId)));
        messageAttributes.put("resourceId", new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(String.valueOf(resourceId)));

        // Value
        messageAttributes.put("Value", new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(value));

        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(System.getenv("AWS_QUEUE_URL"))
                .withMessageAttributes(messageAttributes)
                .withMessageGroupId(registration.getEndpoint())
                .withMessageDeduplicationId(UUID.randomUUID().toString())
                .withMessageBody("notification")
                ;

        sqs.sendMessage(send_msg_request);
    }
}
