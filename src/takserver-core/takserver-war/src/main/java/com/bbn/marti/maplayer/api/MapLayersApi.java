package com.bbn.marti.maplayer.api;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.maplayer.repository.MapLayerRepository;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.google.common.base.Strings;

import io.swagger.annotations.Api;
import tak.server.Constants;

/*
 *
 * REST API for adding and retrieving Map Layers.
 *
 */
@RestController
@Api(value = "MapLayers")
public class MapLayersApi extends BaseRestController {

	@Autowired
    private MapLayerRepository mapLayerRepository;

    /*
     * Get all Map Layers
     */
    @RequestMapping(value = "/maplayers/all", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    ApiResponse<Collection<MapLayer>> getAllMapLayers() throws RemoteException {

        return new ApiResponse<Collection<MapLayer>>(Constants.API_VERSION, MapLayer.class.getName(),
                mapLayerRepository.findAll(Sort.by("name")));
    }


    /*
     * Create a Map Layer
     */
    @RequestMapping(value = "/maplayers", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    ApiResponse<MapLayer> createMapLayer(@RequestBody MapLayer mapLayer) {
        String uid = UUID.randomUUID().toString().replace("-", "");
        mapLayer.setUid(uid);
        mapLayer.setCreateTime(new Date());
        mapLayer.setModifiedTime(new Date());
        MapLayer newMapLayer;

        if (mapLayer.isDefaultLayer()) {
           mapLayerRepository.unsetDefault();
        }
        try {

            newMapLayer = mapLayerRepository.save(mapLayer);

        } catch (Exception e) {
            throw new TakException("exception in createMapLayer", e);
        }

        return new ApiResponse<>(Constants.API_VERSION, "MapLayer", newMapLayer);
    }

    /*
     * Get Map Layer per uid
     */

    @RequestMapping(value = "/maplayers/{uid}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    ApiResponse<MapLayer> getMapLayerForUid(@PathVariable("uid") @NotNull String uid) throws RemoteException {
        if (Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException("UID must be specified");
        }
        MapLayer mapLayer = mapLayerRepository.findByUid(uid);

        if (mapLayer == null) {
            throw new NotFoundException("no map layer stored for uid " + uid);
        }

        return new ApiResponse<>(Constants.API_VERSION, "MapLayer", mapLayer);
    }

    /*
     * Delete a Map Layer for a uid
     */
    @RequestMapping(value = "/maplayers/{uid}", method = RequestMethod.DELETE)
    public void deleteMapLayer(@PathVariable("uid") @NotNull String uid) throws RemoteException {

        if (Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException("UID must be specified");
        }
        try {
            mapLayerRepository.deleteByUid(uid);
        } catch (Exception e) {
            throw new TakException("exception in deleteMapLayer", e);
        }
    }

    /**
     * Update a map layer
     * @param modMapLayer
     * @return Updated MapLayer
     * @throws RemoteException
     */
    @RequestMapping(value = "/maplayers", method = RequestMethod.PUT)
    ApiResponse<MapLayer> updateMapLayer(@RequestBody MapLayer modMapLayer) throws RemoteException {

        MapLayer updatedMapLayer; // result set returned
        String uid = modMapLayer.getUid();
        try {
            MapLayer record = mapLayerRepository.findByUid(uid);
            if (record == null) {
                throw new NotFoundException("no map layer stored for uid " + uid);
            }
            // if the new layer is the default, unset all the others
            if (modMapLayer.isDefaultLayer()) {
                mapLayerRepository.unsetDefault();
            }
            record.setCreatorUid(modMapLayer.getCreatorUid());
            record.setName(modMapLayer.getName());
            record.setDescription(modMapLayer.getDescription());
            record.setType(modMapLayer.getType());
            record.setUrl(modMapLayer.getUrl());
            record.setModifiedTime(new Date());
            record.setDefaultLayer(modMapLayer.isDefaultLayer());
            record.setEnabled(modMapLayer.isEnabled());

            updatedMapLayer = mapLayerRepository.save(record);
        } catch (Exception e) {
            throw new TakException("exception in updateMapLayer", e);
        }
        return new ApiResponse<>(Constants.API_VERSION, "MapLayer", updatedMapLayer);
    }
}