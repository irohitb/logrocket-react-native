require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "logrocket-react-native"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.license      = "MIT"

  s.authors      = package["author"]
  s.platforms    = { :ios => "10.0" }
  s.homepage     = package["repository"]["baseUrl"]
  s.source       = { :git => 'https://github.com/AppHub/LogRocket.git' }

  s.source_files = "ios/**/*.{h,c,m,swift}"
  s.requires_arc = true

  s.dependency "LogRocket", "0.23.1"
  s.dependency "React"
end

# vi: ft=ruby
