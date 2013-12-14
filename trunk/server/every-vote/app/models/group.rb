class Group
  include Mongoid::Document

  # data
  field :name, type: String
  
  # relations
  has_and_belongs_to_many :group_users, class_name: User.name, inverse_of: :in_groups
  belongs_to :user, class_name: User.name, inverse_of: :groups
end