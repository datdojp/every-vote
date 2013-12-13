class Request
  include Mongoid::Document



  belongs_to :user, class_name: User.name, inverse_of: :requests
end