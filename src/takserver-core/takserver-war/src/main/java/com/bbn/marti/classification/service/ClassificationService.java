package com.bbn.marti.classification.service;

import com.bbn.marti.remote.groups.UserClassification;

public interface ClassificationService {

    boolean canAccess(UserClassification userClassification, String itemClassification);

}
