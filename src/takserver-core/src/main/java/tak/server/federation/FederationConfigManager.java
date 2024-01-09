package tak.server.federation;

import java.io.File;
import java.rmi.RemoteException;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Federation;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.FederationConfigInfo;
import com.bbn.marti.remote.FederationConfigInterface;

public class FederationConfigManager implements FederationConfigInterface {

    private static final long serialVersionUID = 2880902388047468469L;

    public FederationConfigManager() throws RemoteException {
    }

    private static final Logger logger = LoggerFactory.getLogger(FederationConfigManager.class);


    @Override
    public FederationConfigInfo getFederationConfig() throws RemoteException {
        Federation fedConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation();
        if (fedConfig.getFederationServer().getFederationPort().isEmpty()) {
            Federation.FederationServer.FederationPort p = new Federation.FederationServer.FederationPort();
            p.setPort(fedConfig.getFederationServer().getPort());
            p.setTlsVersion(fedConfig.getFederationServer().getTls().getContext());
            fedConfig.getFederationServer().getFederationPort().add(p);
        }

        Federation.MissionDisruptionTolerance mdt = fedConfig.getMissionDisruptionTolerance();
        if (mdt == null) {
            mdt = new Federation.MissionDisruptionTolerance();
        }

        return new FederationConfigInfo(fedConfig.isEnableFederation(),
                                        fedConfig.getFederationServer().getV1Tls(),
                                        fedConfig.getFederationServer().getFederationPort(),
                                        fedConfig.getFederationServer().getPort(),
                                        fedConfig.getFederationServer().getV2Port(),
                                        fedConfig.getFederationServer().isV1Enabled(),
                                        fedConfig.getFederationServer().isV2Enabled(),
                                        fedConfig.getFederationServer().getTls().getTruststoreFile(),
                                        fedConfig.getFederationServer().getTls().getTruststorePass(),
                                        fedConfig.getFederationServer().getTls().getContext(),
                                        fedConfig.getFederationServer().getWebBaseUrl(),
                                        fedConfig.isAllowMissionFederation(),
                                        fedConfig.isAllowDataFeedFederation(),
                                        fedConfig.isAllowFederatedDelete(),
                                        fedConfig.isEnableMissionFederationDisruptionTolerance(),
                                        fedConfig.getMissionFederationDisruptionToleranceRecencySeconds(),
                                        mdt.getMission(),
                                        fedConfig.getFederationServer().getCoreVersion(),
                                        fedConfig.isFederatedGroupMapping(),
                                        fedConfig.isAutomaticGroupMapping(),
                                        fedConfig.isEnableDataPackageAndMissionFileFilter(),
                                        fedConfig.getFileFilter().getFileExtension());
    }

    @Override
    public void modifyFederationConfig(FederationConfigInfo info) throws RemoteException {
        Federation fedConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation();

        if (logger.isDebugEnabled()) {
            logger.debug("config ident: " + System.identityHashCode(CoreConfigFacade.getInstance()));
        }

        fedConfig.setEnableFederation(info.isEnabled());
        fedConfig.getFederationServer().setPort(info.getServerPortv1());
        fedConfig.getFederationServer().setV2Port(info.getServerPortv2());
        fedConfig.getFederationServer().setV1Enabled(info.isServerPortEnabled());
        fedConfig.getFederationServer().setV2Enabled(info.isServerPortEnabledv2());
        fedConfig.getFederationServer().getTls().setTruststoreFile(info.getTruststorePath());
        fedConfig.getFederationServer().getTls().setTruststorePass(info.getTruststorePass());
        fedConfig.getFederationServer().getTls().setContext(info.getTlsVersion());
        fedConfig.getFederationServer().setWebBaseUrl(info.getWebBaseURL());
        fedConfig.setAllowMissionFederation(info.isAllowMissionFederation());
        fedConfig.setAllowDataFeedFederation(info.isAllowDataFeedFederation());
        fedConfig.setAllowFederatedDelete(info.isAllowFederatedDelete());
        fedConfig.setEnableMissionFederationDisruptionTolerance(info.isEnableMissionFederationDisruptionTolerance());
        fedConfig.setMissionFederationDisruptionToleranceRecencySeconds(info.getMissionFederationDisruptionToleranceRecencySeconds());
        if (info.getMissionInterval().size() == 0) {
            fedConfig.setMissionDisruptionTolerance(null);
        } else {
            if (fedConfig.getMissionDisruptionTolerance() == null) {
                fedConfig.setMissionDisruptionTolerance(new Federation.MissionDisruptionTolerance());
            }
            fedConfig.getMissionDisruptionTolerance().getMission().clear();
            fedConfig.getMissionDisruptionTolerance().getMission().addAll(info.getMissionInterval());
        }
        fedConfig.getFederationServer().setCoreVersion(info.getCoreVersion());
        fedConfig.getFederationServer().getV1Tls().clear();
        fedConfig.getFederationServer().getV1Tls().addAll(info.getV1Tls());
        fedConfig.getFederationServer().getFederationPort().clear();
        fedConfig.getFederationServer().getFederationPort().addAll(info.getV1Ports());
        fedConfig.setFederatedGroupMapping(info.isFederatedGroupMapping());
        fedConfig.setAutomaticGroupMapping(info.isAutomaticGroupMapping());
        fedConfig.setEnableDataPackageAndMissionFileFilter(info.isEnableDataPackageAndMissionFileFilter());
        fedConfig.getFileFilter().getFileExtension().clear();
        fedConfig.getFileFilter().getFileExtension().addAll(info.getFileExtension());

        if (logger.isDebugEnabled()) {
            logger.debug("is federation enabled? " + info.isEnabled() + ", " + fedConfig.isEnableFederation());
        }
        CoreConfigFacade.getInstance().setAndSaveFederation(fedConfig);
        if (logger.isDebugEnabled()) {
            logger.debug("changes saved?");
        }
    }

    @Override
    public boolean verifyFederationTruststore() throws RemoteException {
        Federation fedConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation();

        String fileName = fedConfig.getFederationServer().getTls().getTruststoreFile();
        File truststoreFileObj = new File(fileName);
        if (truststoreFileObj.exists()) {
            return true;
        } else {
            return false;
        }
    }
}
