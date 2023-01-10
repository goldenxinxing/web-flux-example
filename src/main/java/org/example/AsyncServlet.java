package org.example;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// todo 该注解在springboot下无效，或者是没发现别的机制
// @WebServlet(urlPatterns = "/asyncservlet", asyncSupported = true)

/**
 * 主要作用就是把server端servlet的thread释放出来，弄一个新线程，去从input读或response写，
 * 进而将container中的thread释放出来，能够再处理更多的这样的连接，不至于卡死web container（如典型的tomcat）
 * 好像servlet 3.1已经支持（todo 在头里加ASYNC，而开发者不用管？ 待验证）
 * 深入想一下，
 *  1. tomcat是什么？实现了http协议解析并基于servlet规范实现
 *  2. webmvc是什么？实现了具体的servlet，并且基于spring-web规范实现
 *  初步总结，他俩和一起能够使用spring mvc，抽象层次不同
 *  3. webflux是什么？实现了具体的reactive，并且基于spring-web规范实现
 *  所以，
 *      第一点，webflux不排斥servlet，只要做好adapter就行
 *      第二点，没想通的是，controller和flux提供者该如何写?
 *          flux提供者需要stream式/async式提供，不能在main中实现，否则就是sync
 *      第三点，基于reactive的webflux在：
 *          netty下，todo
 *          servlet-container下，在具体的servlet实现基础上，尽早的释放了原先会block的thread（web container的http-nio命名的线程池中线程），
 *          将result处理过程改为pub-sub的方式，并在处理完后对response做commit处理（与servlet的async机制类似，只不过抽象层级为spring-web）
 * 感觉不太本质，所以reactive才是本质
 */
@Slf4j
public class AsyncServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {
        response.setContentType("text/html;charset=UTF-8");
        final AsyncContext acontext = request.startAsync();
        log.info("main, thread:{}", Thread.currentThread());
        acontext.start(new Runnable() {
            @SneakyThrows
            public void run() {
                String param = acontext.getRequest().getParameter("param");
                log.info("async,param:{}, thread:{}", param, Thread.currentThread());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ServletResponse response = acontext.getResponse();
                /* ... print to the response ... */
                response.getOutputStream().write("success1".getBytes());
                // 执行此，会立即将数据发送给client
                response.getOutputStream().flush();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                response.getOutputStream().write("success2".getBytes());
                // 未执行flush，故会与3一起返回
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                response.getOutputStream().write("success3".getBytes());
                // 以上print信息：说明是异步
                // 2023-01-07 11:06:15.398  INFO 32164 --- [nio-8080-exec-1] org.example.AsyncServlet                 : main, thread:Thread[http-nio-8080-exec-1,5,main]
                // 2023-01-07 11:06:15.404  INFO 32164 --- [nio-8080-exec-2] org.example.AsyncServlet                 : async,param:gxx, thread:Thread[http-nio-8080-exec-2,5,main]
                // 中间是10s的等待
                // 2023-01-07 11:06:45.894  INFO 32164 --- [nio-8080-exec-3] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
                // 2023-01-07 11:06:45.894  INFO 32164 --- [nio-8080-exec-3] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
                // 2023-01-07 11:06:45.897  INFO 32164 --- [nio-8080-exec-3] o.s.web.servlet.DispatcherServlet        : Completed initialization in 2 ms
                // 2023-01-07 11:06:45.989 ERROR 32164 --- [nio-8080-exec-3] s.e.ErrorMvcAutoConfiguration$StaticView : Cannot render error page for request [/asyncservlet] as the response has already been committed. As a result, the response may have the wrong status code.
                // 此处无关
                response.getOutputStream().flush();
                // 主要是此处complete能及时告知完成了（todo timeout=20s后，也会由client端发起complete？？）
                acontext.complete();
            }
        });
    }
}