

package com.bbn.marti.kml.icon.service;


public interface IconsetUploadProcessor {
    void process(String filename);
    
    void setUsername(String username);
    
    void setRoles(String roles);
    
    void setRequestPath(String requestPath);
}
