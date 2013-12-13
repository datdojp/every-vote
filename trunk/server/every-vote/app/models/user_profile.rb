class UserProfile
  include Mongoid::Document

  field :service, type: String
  field :user_name, type: String
  field :access_token, type: String
  field :refresh_token, type: String

  embedded_in :user, class_name: User.name, inverse_of: :sns_profiles
end