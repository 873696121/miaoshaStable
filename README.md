# 秒杀项目记录

## 秒杀项目云端部署

### 云主机准备

![](https://gitee.com/hu873696121/blog-img/raw/master/img/20210708195219.png)

首先按量付费购买四台云主机，先对其中一台操作，准备将项目部署到这一台服务器上。

![image-20210708195531087](https://gitee.com/hu873696121/blog-img/raw/master/img/20210708195531.png)

要准备的工具包如上。

#### 安装java

将jdk传送至服务器上并安装

```shell
# 修改权限
chmod 777 jdk-8u65-linux-x64.rpm
# 安装jdk
rpm -ivh jdk-8u65-linux-x64.rpm
# 修改环境变量
cd ~
vim .bash_profile
```

添加环境变量

```bash
PATH=$PATH:$HOME/bin
JAVA_HOME=//usr/java/jdk1.8.0_65
PATH=$PATH:$JAVA_HOME/bin
export PATH
```

使之生效

```bash
source .bash_profile
```



#### 安装mysql5

```shell
yum install mysql*
yum install mariadb-server
systemctl start mariadb.service
mysqladmin -u root password root
```

设置远端可以访问

执行以下sql语句

```sql
grant all privileges on *.* to root@'%' identified by 'root';
flush privileges;
```

#### 数据库插入数据

使用navicat连接数据库，运行sql文件，即创建了miaosha数据库



### 打包上传

进入项目根目录下，执行打包命令

```
mvn clean package
```

在target文件夹下生成了jar包

![image-20210708201310630](https://gitee.com/hu873696121/blog-img/raw/master/img/20210708201310.png)



将jar包上传到服务器，存放到www/java/miaosha目录下

运行jar包

```bash
java -jar miaosha-1.0-SNAPSHOT.jar
```

即可启动服务器。

### 挂载配置文件

在miaosha目录下新建application.properties，覆盖项目中的配置

```bash
vim application.properties
```

修改端口为80

```
server.port = 80
```

重新运行服务器

```bash
java -jar miaosha-1.0-SNAPSHOT.jar --spring.config.addition-location=/www/java/miaosha/application.properties
```

### 编写启动脚本

```bash
nohup java -Xms400m -Xmx400m -XX:NewSize=200m -XX:MaxNewSize=200m -jar miaosha.jar --spring.config.addition-location=/www/java/miaosha/application.properties
```

此时项目可后台运行

使用如下命令查看日志

```bash
tail -200f nohup.out
```

修改hosts 

```
miaosha 101.34.123.165
```



## Jmeter性能初测

### jmeter安装启动

解压apache-jmeter-5.0.tgz

进入bin目录

```bash
./jmeter.sh
```

启动jmeter

### Jmeter参数设置

![image-20210708202843283](https://gitee.com/hu873696121/blog-img/raw/master/img/20210708202843.png)

### 问题初探： 容量问题

发现问题 : server端并发线程数上不去

### 原因和解决

原因是springboot内嵌的tomcat默认设置的线程数太少，需要手动修改

![image-20210708203155181](https://gitee.com/hu873696121/blog-img/raw/master/img/20210708203155.png)

修改application.properties

```
server.port = 80server.tomcat.accept-count = 1000server.tomcat.max-threads = 800server.tomcat.min-spare-threads = 100
```

这样最小线程数为100，最大线程数可以达到1000

### 压测结果

压测参数

![image-20210708204155194](https://gitee.com/hu873696121/blog-img/raw/master/img/20210708204155.png)

压测结果

![image-20210708204645260](https://gitee.com/hu873696121/blog-img/raw/master/img/20210708204648.png)

![image-20210708204518176](https://gitee.com/hu873696121/blog-img/raw/master/img/20210708204518.png)

 可以看出不做任何优化的情况下，大概能支持200+的并发，主要性能消耗在了mysql上



![image-20210824231514818](https://gitee.com/hu873696121/blog-img/raw/master/img/image-20210824231514818.png)

## 分布式扩展

### 部署两台java应用服务器

修改mysql数据库地址

在秒杀目录下

```shel
vim application.properties
```

覆盖数据库地址

```
spring.datasource.url=jdbc:mysql://172.17.0.3:3306/miaosha?useUnicode=true&characterEncoding=UTF-8
```



将jdk文件和miaosha文件夹拷贝到两台应用服务器上

```bash
scp -r www root@172.17.0.15:/scp -r jdk-8u65-linux-x64.rpm  root@172.17.0.15:/# 连接ssh root@172.17.0.15cd /chmod -R 777 jdk-8u65-linux-x64.rpmrpm -ivh jdk-8u65-linux-x64.rpm cd /www/java/miaosha./deploy.sh &
```

```
scp -r jdk-8u65-linux-x64.rpm  root@172.17.0.15:/
```

```
rpm -ivh jdk-8u65-linux-x64.rpm 
```

```
cd www/java/miaosha./deploy.sh &
```



### 修改前端资源

增加getHost.js文件，里面定义了要访问的服务器地址，然后让所有前端资源指向这个地址，这样可以方便的进行本地调试



### 部署nginx服务器

#### 安装

上传openresty-1.13.6.2.tar.gz文件

```shell
scp openresty-1.13.6.2.tar.gz root@ip:/upload
```

连接服务器

```shell
cd /uploadchmod -R 777 openresty-1.13.6.2.tar.gz
tar -zxvf openresty-1.13.6.2.tar.gz
cd openresty-1.13.6.2/
yum install pcre-devel openssl-devel gcc curl
./configure
make
make install
cd /usr/local/openresty/
ls
cd nginx/
# 启动nginx服务器
sbin/nginx -c conf/nginx.conf
```

默认开启在80端口，浏览器输入ip地址即可访问

![image-20210709154700002](https://gitee.com/hu873696121/blog-img/raw/master/img/20210709154700.png)

#### 上传前端代码

进入前端代码根目录下

```shell
scp -r * root@1.116.139.185:/usr/local/openresty/nginx/html/
```



#### 部署静态资源请求



```shell
cd nginx/confvim nginx.conf
```





```
location /resources/  {            alias  /usr/local/openresty/nginx/html/resources;            index  index.html index.htm;        }
```



将html文件夹下的所有文件放到html/resources下

重启nginx

```shell
# 在openresty根目录下sbin/nignx -s reload
```



#### 设置动态资源反向代理

```
upstream backend_server{        server 172.17.0.15 weight=1;        server 172.17.0.14 weight=1;}location  /  {          proxy_pass http://backend_server;          proxy_set_header Host $http_host:$proxy_port;          proxy_set_header X-Real-IP $remote_addr;          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;        }
```





#### 开启tomcat access log验证

在服务器miaosha目录下新建tomcat目录，并赋予写权限

```shel
cd /www/java/miaoshamkdir tomcatvim application.properties
```

添加配置

```
server.tomcat.accesslog.enabled=true
server.tomcat.accesslog.directory=/www/java/miaosha/tomcat
server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D
```



对另一台做相同设置

```shel
scp application.properties root@172.17.0.15:/www/java/miaoshassh root@172.17.0.15cd /www/java/miaoshamkdir tomcatchmod -R 777 tomcat/ps -ef | grep javakill xxxx./deploy.sh &
```



#### 修改host地址

将nginx





#### 性能优化对比

![image-20210709174427944](https://gitee.com/hu873696121/blog-img/raw/master/img/20210709174428.png)

优化前：

将请求直接打到mysql服务器上

![image-20210709174457727](https://gitee.com/hu873696121/blog-img/raw/master/img/20210709174458.png)

![image-20210709174601524](https://gitee.com/hu873696121/blog-img/raw/master/img/20210709174601.png)

优化后：

将请求打到nginx服务器上

![image-20210709174759690](https://gitee.com/hu873696121/blog-img/raw/master/img/20210709174759.png)

![image-20210709175152744](https://gitee.com/hu873696121/blog-img/raw/master/img/20210709175152.png)

![image-20210825003351606](https://gitee.com/hu873696121/blog-img/raw/master/img/image-20210825003351606.png)

#### 长连接优化

ningx与应用服务器默认是短连接，这不科学，应设置为长连接

```
ssh root@1.116.139.185cd /usr/local/openresty/nginx/vim conf/nginx.conf
```



```
upstream backend_server{        server 172.17.0.15 weight=1;        server 172.17.0.14 weight=1;        keepalive 16;}location  /  {          proxy_pass http://backend_server;          proxy_set_header Host $http_host:$proxy_port;          proxy_set_header Connection "";          proxy_set_header X-Real-IP $remote_addr;          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;          proxy_http_version 1.1;        }
```

```shell
sbin/nginx -s reload
```



#### 启动项目步骤

##### mysql主机

```shell
#mysql
systemctl start mariadb.service
#redis
cd /upload/redis-4.0.1
src/redis-server ./redis.conf &
```

##### java

```shell
cd /www/miaosha/java/
./deploy.sh &
```

##### nginx

```shell
cd /usr/local/openresty/nginx
sbin/nginx -c conf/nginx.conf
```



#### 重新部署

```shell
#上传
scp miaosha-1.0-SNAPSHOT.jar root@192.168.1.41:/upload
#部署
cd /upload/
mv miaosha-1.0-SNAPSHOT.jar /www/miaosha/java/miaosha.jar
y
cd /www/miaosha/java/
ps -ef | grep java
kill ...
./deploy.sh &

```





## 分布式session







## 多级缓存

缓存是提高性能的主要手段。此部分使用redis缓存，热点内存本地缓存，nginx proxy cache缓存和nginx lua缓存来优化项目。

### redis缓存

![image-20210825221213190](https://gitee.com/hu873696121/blog-img/raw/master/img/image-20210825221213190.png)



guava cache



![image-20210825223908318](https://gitee.com/hu873696121/blog-img/raw/master/img/image-20210825223908318.png)



静态页面

![image-20210825224316856](https://gitee.com/hu873696121/blog-img/raw/master/img/image-20210825224316856.png)



## 页面静态化

### CDN静态化

引入cdn让静态页面传输更快

### 全页面静态化

使用爬虫技术，将ajax请求得到的数据连同静态资源缓存成一个html文件，实现全页面静态化





## 交易优化

![image-20210826001204030](https://gitee.com/hu873696121/blog-img/raw/master/img/image-20210826001204030.png)



