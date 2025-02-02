package com.avast.gradle.dockercompose

import spock.lang.Specification

class DockerExecutorTest extends Specification {

    def "reads Docker platform"() {
        def f = Fixture.plain()
        when:
        String dockerPlatform = f.extension.dockerExecutor.getDockerPlatform()
        then:
        noExceptionThrown()
        !dockerPlatform.empty
    }

    def "reads network gateway"() {
        def f = Fixture.withNginx()
        when:
        f.project.tasks.composeBuild.build()
        f.project.tasks.composeUp.up()
        ServiceInfo serviceInfo = f.project.tasks.composeUp.servicesInfos.find().value
        String networkName = serviceInfo.firstContainer.inspection.NetworkSettings.Networks.find().key
        String networkGateway = f.extension.dockerExecutor.getNetworkGateway(networkName)
        then:
        noExceptionThrown()
        !networkGateway.empty
        cleanup:
        f.project.tasks.composeDown.down()
        f.close()
    }

    def "reads container logs"() {
        def f = Fixture.withHelloWorld()
        f.project.tasks.composeBuild.build()
        f.project.tasks.composeUp.up()
        String containerId = f.extension.servicesInfos.hello.firstContainer.containerId
        when:
        String output = f.extension.dockerExecutor.getContainerLogs(containerId)
        then:
        output.contains('Hello from Docker')
        cleanup:
        f.project.tasks.composeDown.down()
        f.close()
    }

    def "expose service info from nested task"() {
        def f = Fixture.withNginx()
        f.project.plugins.apply 'java'
        f.project.dockerCompose {
            nested { }
        }
        when:
        f.project.tasks.nestedComposeUp.up()
        f.extension.nested.exposeAsSystemProperties(f.project.tasks.test)
        then:
        f.project.tasks.test.properties.systemProperties.containsKey('web.host')
        f.project.tasks.test.properties.systemProperties.containsKey('web.tcp.80')
        cleanup:
        f.project.tasks.nestedComposeDown.down()
        f.close()
    }
}
