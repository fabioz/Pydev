from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app


class MainPage(webapp.RequestHandler):
    
    
    def get(self):
        user = users.get_current_user()

        if user:
            self.response.out.write(
                'Hello %s <a href="%s">Sign out</a><br>Is administrator: %s' % 
                (user.nickname(), users.create_logout_url("/"), users.is_current_user_admin())
            )
        else:
            self.redirect(users.create_login_url(self.request.uri))


app = webapp.WSGIApplication([('/', MainPage)], debug=True)


def main():
    run_wsgi_app(app)


if __name__ == "__main__":
    main()
