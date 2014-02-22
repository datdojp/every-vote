class SnsAccount
  include Mongoid::Document

  field :sns_id, type: String
  field :type, type: String
  field :access_token, type: String

  belongs_to :user, class_name: User.name, inverse_of: :sns_accounts
end