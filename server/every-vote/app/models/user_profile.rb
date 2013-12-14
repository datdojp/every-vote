class UserProfile
  include Mongoid::Document

  # data
  field :service, type: String
  field :user_name, type: String
  field :access_token, type: String
  field :refresh_token, type: String

  # relations
  embedded_in :user, class_name: User.name, inverse_of: :profiles
end