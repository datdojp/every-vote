class User
  include Mongoid::Document

  field :user_name, type: String

  embeds_many :sns_profiles, class_name: UserProfile.name, inverse_of: :user
  has_many :requests, class_name: Request.name, inverse_of: :user
  has_many :groups, class_name: Group.name, inverse_of: :user
  has_and_belongs_to_many :in_groups, class_name: Group.name, inverse_of: :group_users
end