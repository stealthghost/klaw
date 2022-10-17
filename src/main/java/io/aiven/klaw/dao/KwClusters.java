package io.aiven.klaw.dao;

import io.aiven.klaw.model.KafkaSupportedProtocol;
import java.io.Serializable;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@Entity
@IdClass(KwClusterID.class)
@Table(name = "kwclusters")
public class KwClusters implements Serializable {

  @Id
  @Column(name = "clusterid")
  private Integer clusterId;

  @Id
  @Column(name = "tenantid")
  private Integer tenantId;

  @Column(name = "clustername")
  private String clusterName;

  @Column(name = "bootstrapservers")
  private String bootstrapServers;

  @Column(name = "protocol")
  @Enumerated(EnumType.STRING)
  private KafkaSupportedProtocol protocol;

  @Column(name = "clustertype")
  private String clusterType;

  @Column(name = "sharedcluster")
  private String sharedCluster;

  @Column(name = "publickey")
  private String publicKey;

  @Column(name = "cstatus")
  private String clusterStatus;

  @Column(name = "projectname")
  private String projectName;

  @Column(name = "servicename")
  private String serviceName;

  @Column(name = "kafkaflavor")
  private String kafkaFlavor;
}
