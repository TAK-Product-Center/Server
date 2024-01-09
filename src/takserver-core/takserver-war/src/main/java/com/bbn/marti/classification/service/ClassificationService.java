package com.bbn.marti.classification.service;

import com.bbn.marti.remote.groups.UserClassification;
import tak.server.cot.CotEventContainer;

public interface ClassificationService {

    boolean canAccess(UserClassification userClassification, String itemClassification);

    boolean canAccess(UserClassification userClassification, CotEventContainer cotEventContainer);
}
