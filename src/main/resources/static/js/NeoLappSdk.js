
let lapp = {
    'connection' : {
        invoke ({
            url,
            method = 'GET',
            contentType,
            params,
            data,
            headers
        }) {
            // 返回一个promise对象
            return new Promise((resolve, reject) => {
                // 处理method(转大写)
                method = method.toUpperCase()
                // 处理query参数(拼接到url上)   a=b&c=d
                let queryString = ''
                if(params){
                    if(typeof(params) == 'string'){
                        params = eval ("(" + params + ")");
                        Object.keys(params).forEach(key => {
                            queryString += `${key}=${params[key]}&`
                        })
                    }else{
                        Object.keys(params).forEach(key => {
                            queryString += `${key}=${params[key]}&`
                        })
                    }
                    
                }
                if (queryString) {
                    // 去除最后的&
                    queryString = queryString.substring(0, queryString.length - 1)
                    // 接到url
                    url += '?' + queryString
                }
                // 执行异步ajax请求
                // 创建对象
                const xhr = new XMLHttpRequest()
                // 配置请求信息,参数一是请求的类型,参数二是请求的url,
                // url前面拼接上proxy代理接口
                var proxyUrl = '/service/api/proxy?request=' + url;
                xhr.open(method, proxyUrl, true)
                // 如果用户设置了请求头
                if(headers){
                    if(typeof(headers) == 'string'){
                        headers = eval ("(" + headers + ")");
                        Object.keys(headers).forEach(key => {
                            xhr.setRequestHeader(key,headers[key])
                        })
                    }else{
                        Object.keys(headers).forEach(key => {
                            xhr.setRequestHeader(key,headers[key])
                        })
                    }
                }
                // 发送请求
                if (method === 'GET' || method === 'DELETE') {
                    // 第三步，发送请求
                    xhr.send()
                } else if (method === 'POST' || method === 'PUT') {
                    // data 必须是 Object， 判断data事字符串形式的json还是Object形式的json
                    if(typeof(data) == 'string'){
                        data = eval("(" + data + ")");
                    }
                    // 告诉服务器请求体的格式 如果用户设置请方式使用用户设置的请求方式传递。没设置默认application/json方式
                    if(contentType){ 
                        switch (contentType) {
                          case 'application/x-www-form-urlencoded':
                            var sendData = '';
                            if(data){
                                Object.keys(data).forEach(key => {
                                    sendData += `${key}=${data[key]}&`
                                })
                                if (sendData) {
                                    sendData = sendData.substring(0, sendData.length - 1)
                                }
                            }
                            // 发送a=b&c=d格式数据
                            xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded')
                            xhr.send(sendData) 
                            break;
                          case 'application/json':
                            // 发送json格式请求体参数
                            xhr.setRequestHeader('Content-Type', 'application/json')
                            console.log('json'+JSON.stringify(data))

                            xhr.send(JSON.stringify(data))
                            break;
                          case  'multipart/form-data':
                            var formData = new FormData();
                            Object.keys(data).forEach(key => {
                                var typeResult = Object.prototype.toString.call(data[key]);
                                switch(typeResult){
                                    case '[object String]':
                                        formData.append(key, data[key]);
                                        break;
                                    case '[object File]':
                                        formData.append(key, data[key]);
                                        break;
                                    case '[object FileList]':
                                        Object.keys(data[key]).forEach(item => {
                                            console.log(data[key][item])
                                            formData.append(key, data[key][item]);
                                        })
                                        
                                        break;
                                    case '[object Object]':
                                        formData.append(key, JSON.stringify(data[key]));
                                        break;
                                    case '[object Array]':
                                        formData.append(key, JSON.stringify(data[key]));
                                }
                                console.log(data[key],Object.prototype.toString.call(data[key]))
                            })
                            for (var key of formData.entries()) {
                                    console.log('在上传前查看formData里面的数据'+ key[0] + ', ' + key[1]);
                                }
                            xhr.send(formData)
                        }
                    }else{
                        xhr.setRequestHeader('Content-Type', 'application/json')
                        xhr.send(JSON.stringify(data)) // 发送json格式请求体参数
                    }
                }
                // 绑定状态改变的监听
                xhr.onreadystatechange = function () {
                    // 如果请求没有完成, 直接结束
                    if (xhr.readyState !== 4) {
                        return
                    }
                    // 如果响应状态码在[200, 300)之间代表成功, 否则失败
                    const {status, statusText} = xhr
                    // 获取返回的数据
                    // 如果请求成功了, 调用resolve()
                    if (status >= 200 && status <= 299) {
                        // 准备结果数据对象response
                        const response = {
                            data: JSON.parse(xhr.response),
                            status,
                            statusText
                        }
                        resolve(response)
                    } else { // 如果请求失败了, 调用reject()
                        reject(new Error('请求出错' + status))
                    }
                }
            })
        },
    },
}