package com.example.licenseservice.services;

import com.example.licenseservice.clients.OrganizationRestTemplateClient;
import com.example.licenseservice.config.ServiceConfig;
import com.example.licenseservice.model.License;
import com.example.licenseservice.model.Organization;
import com.example.licenseservice.repository.LicenseRepository;
import com.example.licenseservice.utils.UserContextHolder;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class LicenseService {
    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);
    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    ServiceConfig config;

    @Autowired
    OrganizationRestTemplateClient organizationRestClient;



    public License getLicense(String organizationId,String licenseId, String clientType) {
        License license = licenseRepository.findByOrganizationIdAndLicenseId(organizationId, licenseId);

        Organization org = getOrganization(organizationId);

        return license
                .withOrganizationName( org.getName())
                .withContactName( org.getContactName())
                .withContactEmail( org.getContactEmail() )
                .withContactPhone( org.getContactPhone() )
                .withComment(config.getExampleProperty());
    }

    @HystrixCommand
    private Organization getOrganization(String organizationId) {
        return organizationRestClient.getOrganization(organizationId);
    }

    private void randomlyRunLong(){
        Random rand = new Random();

        int randomNum = rand.nextInt((3 - 1) + 1) + 1;

        if (randomNum==3) sleep();
    }

    private void sleep(){
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @HystrixCommand(//fallbackMethod = "buildFallbackLicenseList",
            threadPoolKey = "licenseByOrgThreadPool",
            threadPoolProperties =
                    {@HystrixProperty(name = "coreSize",value="30"),
                            @HystrixProperty(name="maxQueueSize", value="10")},
            commandProperties={
                    @HystrixProperty(name="circuitBreaker.requestVolumeThreshold", value="10"),
                    @HystrixProperty(name="circuitBreaker.errorThresholdPercentage", value="75"),
                    @HystrixProperty(name="circuitBreaker.sleepWindowInMilliseconds", value="7000"),
                    @HystrixProperty(name="metrics.rollingStats.timeInMilliseconds", value="15000"),
                    @HystrixProperty(name="metrics.rollingStats.numBuckets", value="5")}
    )
    public List<License> getLicensesByOrg(String organizationId){
        logger.debug("LicenseService.getLicensesByOrg  Correlation id: {}", UserContextHolder.getContext().getCorrelationId());
        return licenseRepository.findByOrganizationId( organizationId );
    }

    private List<License> buildFallbackLicenseList(String organizationId){
        List<License> fallbackList = new ArrayList<>();
        License license = new License()
                .withId("0000000-00-00000")
                .withOrganizationId( organizationId )
                .withProductName("Sorry no licensing information currently available");

        fallbackList.add(license);
        return fallbackList;
    }

    public void saveLicense(License license){
        license.withId( UUID.randomUUID().toString());

        licenseRepository.save(license);

    }

    public void updateLicense(License license){
        licenseRepository.save(license);
    }

    public void deleteLicense(License license){
        licenseRepository.delete(license);
    }
}
