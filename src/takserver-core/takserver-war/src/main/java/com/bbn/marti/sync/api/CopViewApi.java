package com.bbn.marti.sync.api;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.Configuration;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.sync.model.CopHierarchyNode;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;
import com.google.common.base.Strings;

import tak.server.Constants;


@RestController
public class CopViewApi extends BaseRestController {

	private static final Logger logger = LoggerFactory.getLogger(CopViewApi.class);

	// keep a reference to the currently active request
	@Autowired
	private HttpServletRequest request;

	@Autowired
	private HttpServletResponse response;

	@Autowired
	private MissionService missionService;

	@Autowired
	private MissionRepository missionRepository;

	@Autowired
	private CommonUtil martiUtil;

	@Autowired
	private CoreConfig config;

	private static String mcsCopTool = "";


	/*
	 * get all cops
	 */
	@RequestMapping(value = "/cops", method = RequestMethod.GET)
	Callable<ApiResponse<List<Mission>>> getAllCopMissions(
			@RequestParam(value = "path", required = false) String path,
			@RequestParam(value = "offset", required = false) Integer offset,
			@RequestParam(value = "size", required = false) Integer size) throws RemoteException {

		if (logger.isDebugEnabled()) {
			logger.debug("cop API getAllCops");
		}

		NavigableSet<Group> groups = martiUtil.getGroupsFromRequest(request);

		List<Mission> missions = missionService.getAllCopsMissions(getDefaultMcsTool(), groups, path, offset, size);

		return () -> {

			return new ApiResponse<List<Mission>>(Constants.API_VERSION, Mission.class.getSimpleName(), missions);
		};
	}


	@RequestMapping(value = "/cops/hierarchy", method = RequestMethod.GET)
	public ApiResponse<Set<CopHierarchyNode>> getHierarchy(HttpServletRequest request)
			throws ValidationException, IntrusionException, RemoteException {

		// get the set of paths that this user can see
		List<String> paths = missionRepository.getMissionPathsByTool(
				getDefaultMcsTool(), martiUtil.getGroupVectorBitString(request));

		// organize the paths into a cop hierarchy and return
		HashMap<String, CopHierarchyNode> nodeMap = new HashMap<>();
		CopHierarchyNode root = new CopHierarchyNode("");
		for (String path : paths) {
			CopHierarchyNode parent = root;
			String tmpPath = "";
			for (String level : path.split("/")) {
				tmpPath += "/" + level;
				CopHierarchyNode node = nodeMap.get(tmpPath);
				if (node == null) {
					node = new CopHierarchyNode(level);
					nodeMap.put(tmpPath, node);
					parent.addChild(node);
				}
				parent = node;
			}
		}

		return new ApiResponse<Set<CopHierarchyNode>>(
				Constants.API_VERSION, CopHierarchyNode.class.getSimpleName(), root.getChildren());
	}

	public String getDefaultMcsTool() {
		if (Strings.isNullOrEmpty(mcsCopTool)) {
			try {
				Configuration conf = config.getRemoteConfiguration();
				mcsCopTool = conf.getNetwork().getMissionCopTool();
			} catch(Exception ex) {
				logger.info("Failed to get default mission cop tool. Using \"vbm\"");
				mcsCopTool = "vbm";
			}
		}
		return mcsCopTool;
	}
}
