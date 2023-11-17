# Ingress Infrastructure Directory

This directory contains Ingress infrastructure files which are referenced by the python script files. 
This directory's files are at the operations level and therefore they are kept outside the [Helm Chart](../helm).   


##Upgrade Process

1.  Clone the repository `https://github.com/kubernetes/ingress-nginx`.  
2.  Check out the version you would like to upgrade to. As of this writing it will be a tag such as 'controller-v.1.8.1'.  
3.  Use a comparison tool to compare ingress-nginx/deploy/static/provider/aws/deploy.yaml to takserver/takserver-cluster/deployments/ingress-infrastructure/ingress-setup.yaml to update our version of the document.  
    * Note there are two sections in`deploy.yaml` that are stored separately in takserver/takserver-cluster/deployments/ingress-infrastructure/load-balancer-deployment.yaml. I would recommend copying them to another file for comparison with the load balancer script following this comparison. Those sections are as follows:  
	  - One with a 'kind' value of 'Deployment'  
	  - One with a 'kind' value of 'Service' AND a 'metadata.name' value of 'ingress-nginx-controller'  
	* There are two custom portions that must be retained:  
	  - A kind=ConfigMap with metadata.name='ingress-nginx-controller' must have 'ssl-protocols: TLSv1.3' included in the data section  
	  - A kind=ConfigMap with metadata.name='tcp-services' must be defined that contains our port mappings  
4.  Compare the file created in the previous step with `takserver/takserver-cluster/deployments/ingress-infrastructure/load-balancer-deployment.yaml.`. You may need to re-order the sections if they do not match up at all.  
