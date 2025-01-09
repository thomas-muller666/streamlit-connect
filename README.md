# StreamlitConnect (WORK-IN-PROGRESS)

## Table of Contents

- [Introduction](#introduction)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Running the StreamlitConnect Server](#running-the-streamlit-server)
    - [Developing a Java Streamlit App](#developing-a-java-streamlit-app)
- [Development](#development)
    - [Architecture Overview](#architecture-overview)
    - [Project Structure](#project-structure)
    - [Adding a New Command](#adding-a-new-command)
    - [Adding a New Component](#adding-a-new-component)
- [Examples](#examples)
- [API Documentation](#api-documentation)
- [Troubleshooting](#troubleshooting)
- [Known Issues](#known-issues)
- [Frequently Asked Questions (FAQs)](#frequently-asked-questions-faqs)
- [Getting Help](#getting-help)
- [How to Contribute](#how-to-contribute)
- [Roadmap](#roadmap)
- [Authors and Acknowledgment](#authors-and-acknowledgment)
- [License](#license)

## Introduction

StreamlitConnect is a robust, highly-flexible interface connecting Streamlit to a remote server using gRPC for communication, enabling you to build Streamlit-like apps in Java and other languages (future scope). This project encapsulates a specialized Streamlit Python server and a Java implementation of the StreamlitConnect link (gRPC server) with a simple API for creating lightweight GUIs in Java.

## Getting Started

Follow these steps...

### Prerequisites

- Java 21 or later

### Installation

To install the StreamlitConnect JAR file to your local Maven repository, download the artifacts from the latest StreamlitConnect release and use the following Maven command:            
```sh
mvn install:install-file \
-Dfile=<path-to-jar-file> \
-DpomFile=<path-to-pom-file> \
-DgroupId=io.streamlitconnect \
-DartifactId=streamlit-connect \
-Dversion=<semantic-version-of-the-release> \
-Dpackaging=jar
```
Ensure to replace the placeholders with the actual path to the downloaded JAR file and the release pom file.

#### Gradle Dependency
To use StreamlitConnect in a Gradle project, add this to your `build.gradle` file's dependencies block: 
```groovy
implementation 'io.streamlitconnect:streamlit-connect:[the-semantic-version-of-the-release]'
```

#### Maven Dependency
To use StreamlitConnect in a Maven project, add this to your `pom.xml` file's dependencies block:
```xml
<dependency>
  <groupId>io.streamlitconnect</groupId>
  <artifactId>streamlit-connect</artifactId>
  <version>[semantic-version-of-the-release]</version>
</dependency>
```

### Running the StreamlitConnect Server

The StreamlitConnect (Python) server is a necessary intermediary between your Streamlit Java app and the front-end. 
This server acts as a gRPC client, and your Java Streamlit application is executed alongside a gRPC server. Every 
request from the front-end is sent to the StreamlitConnect server, which in turn sends a custom gRPC protocol request 
to your Java application. The Java application processes the request, calls your Java streamlit app code, and sends the
response back to the Streamlit server, which then (after some processing) sends the response back to the front-end.

Seen from the front-end, it seems like you're running any old Streamlit app, but in the background, the Java application
is doing the heavy lifting, whilst the Streamlit backend (StreamlitConnect) is acting as a middleman, translating the
front-end Streamlit requests to gRPC requests for the Java application. As a GUI developer you dont deal with the implementation details at all, you only use a simple API to create your GUI. The Java API is a more object-oriented approach than the Python Streamlit API.

To run the StreamlitConnect server in Docker, use the following command:
```sh
docker run -d -p 8501:8501 --name streamlit-connect ttm6666/streamlit-connect:latest
```

This will pull the latest version of the StreamlitConnect server from Docker Hub and run it in a Docker container.

The Docker application can be configured using the following environment variables:
- `STREAMLIT_CONNECT_APP_HOST`: The host where the Java (Streamlit) application is running. Default is `host.docker.internal`.
- `STREAMLIT_CONNECT_APP_PORT`: The port where the Java (Streamlit) application is running. Default is `50051` (default gRPC port).
- `LOG_LEVEL`: The log level of the Streamlit server. Default is `INFO`.

Environment variables can be set using the `-e` flag in the `docker run` command. For example, to set the log level to `DEBUG`, 
use the following command:
```sh
docker run -d -p 8501:8501 --name streamlit-connect -e LOG_LEVEL=DEBUG ttm6666/streamlit-connect:${{ needs.build.outputs.PROJECT_VERSION }}
```

If you are running a bare Docker installation e.g. on WSL2, Mac or Linux, you have to use the actual 
IP-address of the host machine instead of `host.docker.internal` (which is specific to Docker Desktop). Also note that 
port 50051 is the default port for the gRPC server in the Java application. If you change this port (using the 
`io.streamlitconnect.Config` class), you have to adjust the `STREAMLIT_CONNECT_APP_PORT` environment variable accordingly.

After starting your java application and the Docker container with the StreamlitConnect server, you can access your 
Streamlit app at `http://localhost:8501`. Note that the StreamlitConnect server is running on port 8501 (inside Docker), 
which is the default port for Streamlit apps.

### Developing a Java Streamlit App
The most important thing to understand is that, similar as to a regular Streamlit application developed in Python, the Java
application is also executed 'top-down' for every request (change) from the front-end. The better you familiarize yourself with how Streamlit works, the easier it will be to develop them using the Java API provided in this project. 

For furter reading, see [Basic Streamlit Concepts](https://docs.streamlit.io/get-started/fundamentals/main-concepts)

A simple java multi-page application can e.g. look like this:
```java
class TestMultiPageApp implements MultiPageApp {
    
    private static final log Logger = LoggerFactory.getLogger(TestMultiPageApp.class);
    
    public static void main(String[] args) {
        StreamlitAppManager appManager = context -> new TestMultiPageApp();
        AppCache appCache = new AppCache(appManager);
        StreamlitServer server = StreamlitServer.getDefault();
        server.start(appCache);
    }    

    private final LinkButton vgLink = new LinkButton("VG", "https://www.vg.no");
    
    private final Page page1 = new Page() {
        
        private final Button button = new Button("Click me");
        
        @Override
        public String getName() {
          return "page_1";
        }
  
        @Override
        public void render(OperationsRequestContext context) {
          Container root = context.getRootContainer();
          root.title("This is page 1");
          root.widget(new PageLink("page_2", "Go to page 2"));
          root.widget(vgLink);
          root.widget(button);
          if (button.isChanged()) {
              root.text("Button was clicked");
          }
        }
        
    };
    
    private final Page page2 = new Page() {
        
        @Override
        public String getName() {
          return "page_2";
        }
  
        @Override
        public void render(OperationsRequestContext context) {
          Container root = context.getRootContainer();
          root.title("This is page 2");
          root.widget(new PageLink("page_1", "Go to page 1"));
        }
        
    };

    @Override
    public @NotNull Page getPage(@NonNull String name) {
        log.debug("getPage: {}", name);

        return switch (name) {
            case "page_1" -> page1;
            case "page_2" -> page2;
            default -> throw new IllegalArgumentException("Unknown page: " + name);
        };

    }

    @Override
    public NavigationMenu getNavigationMenu(NavigationRequestContext context) {
        NavigationMenu menu = new NavigationMenu();
        NavigationEntry entry1 = new NavigationEntry("page_1", "Page 1", null, false);
        NavigationEntry entry2 = new NavigationEntry("page_2", "Page 2", null, false);
        MenuItem item = new MenuItem("Pages", List.of(entry1, entry2));
        menu.addItems(item);
        return menu;
    }

}

```

## Development

If you want to contribute to the main project, you need to fork and checkout the [StreamlitConnect repository](https://github.com/thomas-muller666/streamlit-connect).

### Architecture Overview

The StreamlitConnect project includes a special purpose Streamlit-based Python server that communicates with a Java (other languages planned in roadmap) remote server using gRPC.

The architecture consists of the following components:

1. **Remote StreamlitConnect backend-server (gRPC Server)**: Receives commands from the Streamlit server (acting like a client seen from the gRPC server) and sends back the results as commands to be executed as Streamlit code.
2. **StreamlitConnect Server (gRPC client)**: Acts as a gRPC client for the remote (StreamlitConnect) server, sending state and fetching commands to be executed as Streamlit code.
3. **Streamlit Frontend**: The Streamlit frontend that displays the Streamlit app and sends commands to the Streamlit server is included in the architecture, but not part of this project - this is part of the official Streamlit project and is just used as is in this project.

The architecture of the project is as follows:

```
+-----------------------------------------+
|                Browser                  | 
| (The official React Streamlit frontend) |
+-----------------------------------------+
                 ^
                 | (gRPC-Web over HTTP/1.1 websocket)
                 V
    +-------------------------+
    | StreamlitConnect server |
    |   (Python gRPC Client)  |
    +-------------------------+
                ^
                | (standard gRPC over HTTP/2)
                V
+--------------------------------------------+
| Remote StreamlitConnect Application server |
|           (Java gRPC Server)               |
+--------------------------------------------+
```

The Java remote StreamlitConnect application backend-server communicates with the StreamlitConnect (Python) server using websockets/gRPC.  The StreamlitConnect server communicates with the Streamlit frontend using websockets/gRPC - but this is actually irrelevant for this project, the Streamlit setup is like any other Streamlit application, only that it fetches the content of the pages from a (Java) server over gRPC.

### Project Structure
TBD

### Adding a New Command
TBD

### Adding a New Component
TBD

## API Documentation

Full API documentation can be found...

## Troubleshooting

Find solutions to some common issues...

## Known Issues

List of known issues...

## Frequently Asked Questions (FAQs)

Some common questions are...

## Getting Help

For bugs and issues...

## How to Contribute

Contribute by...

## Roadmap

Future plans include...

## Authors and Acknowledgment

This project wouldn't have been possible without the contributions of...

## License

This software is licensed under the Apache 2 license.