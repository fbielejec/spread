FROM debian:stretch-slim
MAINTAINER "Filip Bielejec" <fbielejec@gmail.com>

RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q wget nginx

# replace nginx config
COPY services/ui/nginx.conf /etc/nginx/nginx.conf

# replace default server
COPY services/ui/default /etc/nginx/sites-available/default

# nginx config
COPY services/ui/spreadviz.org /etc/nginx/sites-available/spreadviz.org

# setup error page
COPY services/ui/X0X.html /usr/share/nginx/html/X0X.html

# setup site
RUN ln -s -f /etc/nginx/sites-available/spreadviz.org /etc/nginx/sites-enabled/spreadviz.org

# copy static content
COPY services/analysis-viewer/index.html /spread/index.html
COPY services/analysis-viewer/js/main.js /spread/js/main.js
COPY services/analysis-viewer/css/main.css /spread/css/main.css
COPY services/analysis-viewer/fonts/ /spread/fonts/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
