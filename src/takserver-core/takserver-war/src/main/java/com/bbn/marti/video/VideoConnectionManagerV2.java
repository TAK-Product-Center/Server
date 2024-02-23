package com.bbn.marti.video;

import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.util.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class VideoConnectionManagerV2 extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(com.bbn.marti.video.VideoConnectionManagerV2.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private VideoManagerService videoManagerService;

    @Autowired
    private CommonUtil martiUtil;

    @RequestMapping(value = "/video", method = RequestMethod.GET)
    @ResponseBody
    public VideoCollections getVideoCollections(@RequestParam(value = "protocol", required = false) String protocol) {
        try {
            String groupVector = martiUtil.getGroupVectorBitString(request);
            VideoCollections videoCollections = videoManagerService.getVideoCollections(protocol, true, groupVector);
            return videoCollections;
        } catch (Exception e) {
            throw new TakException("exception in getVideoCollections", e);
        }
    }

    @RequestMapping(value = "/video", method = RequestMethod.POST)
    public void createVideoConnection(
            @RequestBody VideoCollections videoCollection) {
        try {
            String groupVector = martiUtil.getGroupVectorBitString(request);
            videoManagerService.createVideoCollections(videoCollection, groupVector);
        } catch (Exception e) {
            throw new TakException("exception in createVideoConnection", e);
        }
    }

    @RequestMapping(value = "/video/{uid}", method = RequestMethod.GET)
    @ResponseBody
    public VideoConnection getVideoConnection(@PathVariable("uid") @NotNull String uid) {
        try {
            String groupVector = martiUtil.getGroupVectorBitString(request);
            VideoConnection videoConnection = videoManagerService.getVideoConnection(uid, groupVector);
            if (videoConnection == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return videoConnection;
        } catch (Exception e) {
            throw new TakException("exception in getVideoConnection", e);
        }
    }

    @RequestMapping(value = "/video/{uid}", method = RequestMethod.PUT)
    public void updateVideoConnection(
            @PathVariable("uid") @NotNull String uid,
            @RequestBody VideoConnection videoConnection) {
        try {
            String groupVector = martiUtil.getGroupVectorBitString(request);
            videoManagerService.updateVideoConnection(videoConnection, groupVector);
        } catch (Exception e) {
            throw new TakException("exception in updateVideoConnection", e);
        }
    }

    @RequestMapping(value = "/video/{uid}", method = RequestMethod.DELETE)
    public void deleteVideoConnection(
            @PathVariable("uid") @NotNull String uid) {
        try {
            String groupVector = martiUtil.getGroupVectorBitString(request);
            videoManagerService.deleteVideoConnection(uid, groupVector);
        } catch (Exception e) {
            throw new TakException("exception in deleteVideoConnection", e);
        }
    }
}

