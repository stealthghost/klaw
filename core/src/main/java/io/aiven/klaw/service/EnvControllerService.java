package io.aiven.klaw.service;

import static io.aiven.klaw.helpers.KwConstants.DATE_TIME_DDMMMYYYY_HHMMSS_FORMATTER;

import io.aiven.klaw.config.ManageDatabase;
import io.aiven.klaw.dao.Env;
import io.aiven.klaw.dao.KwClusters;
import io.aiven.klaw.error.KlawException;
import io.aiven.klaw.model.enums.ClusterStatus;
import io.aiven.klaw.model.enums.KafkaClustersType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@EnableScheduling
@Service
@Slf4j
public class EnvControllerService {

  @Autowired private ClusterApiService clusterApiService;

  @Autowired ManageDatabase manageDatabase;

  // every 1 hour
  @Scheduled(fixedRateString = "PT1H", initialDelay = 60)
  public void loadTenantActiveStatus() {
    try {
      for (Integer tenantId : manageDatabase.getTenantMap().keySet()) {
        long expiryLong = manageDatabase.getTenantFullConfig(tenantId).getLicenseExpiry().getTime();
        long currentTime = System.currentTimeMillis();
        long diffTime = expiryLong - currentTime;
        if (diffTime < 0) {
          manageDatabase.getHandleDbRequests().setTenantActivestatus(tenantId, false);
          manageDatabase.loadOneTenant(tenantId);
        }
      }
    } catch (Exception e) {
      log.info("loadTenantActiveStatus : ", e);
    }
  }

  @Scheduled(fixedRateString = "PT1H", initialDelay = 9000)
  public void loadEnvsWithStatus() {
    updateEnvsStatus();
  }

  public void updateEnvsStatus() {
    log.info("Scheduled job (klaw.reloadclusterstatus.interval) : Update cluster status");
    try {
      Set<Integer> tenants = manageDatabase.getTenantMap().keySet();

      for (Integer tenantId : tenants) {
        List<Env> envList = manageDatabase.getKafkaEnvListAllTenants(tenantId);

        for (Env env : envList) {
          updateEnvStatusPerEnv(tenantId, env);
        }
      }

      for (Integer tenantId : tenants) {
        List<Env> envList = manageDatabase.getSchemaRegEnvList(tenantId);

        for (Env env : envList) {
          updateEnvStatusPerEnv(tenantId, env);
        }
      }

      for (Integer tenantId : tenants) {
        List<Env> envList = manageDatabase.getKafkaConnectEnvList(tenantId);

        for (Env env : envList) {
          updateEnvStatusPerEnv(tenantId, env);
        }
      }
    } catch (Exception e) {
      log.error("Error in loading cluster status ", e);
    }
  }

  private void updateEnvStatusPerEnv(Integer tenantId, Env env) throws KlawException {
    ClusterStatus status;
    LocalDateTime statusTime = LocalDateTime.now(ZoneOffset.UTC);
    KwClusters kwClusters =
        manageDatabase
            .getClusters(KafkaClustersType.of(env.getType()), tenantId)
            .get(env.getClusterId());
    status =
        clusterApiService.getKafkaClusterStatus(
            kwClusters.getBootstrapServers(),
            kwClusters.getProtocol(),
            kwClusters.getClusterName() + kwClusters.getClusterId(),
            env.getType(),
            kwClusters.getKafkaFlavor(),
            tenantId);

    env.setEnvStatus(status);
    kwClusters.setClusterStatus(status);
    env.setEnvStatusTime(statusTime);
    env.setEnvStatusTimeString(DATE_TIME_DDMMMYYYY_HHMMSS_FORMATTER.format(statusTime));
    manageDatabase.addEnvToCache(tenantId, env, true);
    manageDatabase.getHandleDbRequests().addNewCluster(kwClusters);
    manageDatabase.getHandleDbRequests().addNewEnv(env);
  }
}
